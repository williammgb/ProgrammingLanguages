using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;

// simple end-to-end demo: log in, hit the protected service on a timer, and when the
// access token expires, use the refresh token to get a new one and keep going.

const string baseUrl = "http://localhost:5163";
const string username = "alice";
const string password = "Password123!";
var pollInterval = TimeSpan.FromSeconds(5);
var callsToMakeAfterRefresh = 3; // how many more successful calls to make before stopping, after a refresh happens

// Web preset: camelCase + case-insensitive matching, so "accessToken" in the JSON binds to AccessToken
var jsonOptions = new JsonSerializerOptions(JsonSerializerDefaults.Web);

// Expression-bodied syntax is just a concise form of writing a method
void Log(string message) => Console.WriteLine($"[{DateTime.Now:HH:mm:ss}] {message}");

using var http = new HttpClient { BaseAddress = new Uri(baseUrl) };

Log($"Logging in as '{username}'...");

HttpResponseMessage loginResponse;
try
{
    // anonymous object new { ... } becomes JSON, which is then deserialized into LoginRequest by ASP.NET
    loginResponse = await http.PostAsJsonAsync("/api/auth/login", new { username, password });
}
catch (HttpRequestException ex)
{
    Log($"Could not reach the API at {baseUrl} ({ex.Message}). Is it running (dotnet run in MyApp.Api)? Stopping.");
    return;
}

if (!loginResponse.IsSuccessStatusCode)
{
    Log($"Login failed ({(int)loginResponse.StatusCode} {loginResponse.StatusCode}). Is the API running (dotnet run in MyApp.Api)? Stopping.");
    return;
}

var auth = await loginResponse.Content.ReadFromJsonAsync<AuthResult>(jsonOptions);
var accessToken = auth!.AccessToken;
var refreshToken = auth.RefreshToken;
Log("Login succeeded. Received access token + refresh token.");

// hits the protected service with the current access token; true = 200, false = rejected (likely expired)
async Task<bool> CallServiceAsync()
{
    using var request = new HttpRequestMessage(HttpMethod.Get, "/api/service");
    request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);

    var response = await http.SendAsync(request);
    var body = await response.Content.ReadAsStringAsync();

    if (response.IsSuccessStatusCode)
    {
        Log($"Service call succeeded (200): {body}");
        return true;
    }

    Log($"Service call rejected ({(int)response.StatusCode} {response.StatusCode}): {body}");
    return false;
}

var hasRefreshedOnce = false; // flips true the first time we successfully refresh
var callsSinceRefresh = 0; // counts calls after that, to stop once we've proven the new token works

// covers every HTTP call below (service calls + refresh), not just the initial login,
// so a dropped connection mid-loop logs cleanly instead of an unhandled stack trace
try
{
    while (true)
    {
        var ok = await CallServiceAsync();

        if (!ok)
        {
            // already refreshed once and it's still failing: something else is wrong, give up
            if (hasRefreshedOnce)
            {
                Log("Service call failed even after a refresh. Stopping simulation.");
                break;
            }

            Log("Access token was rejected (expired). Requesting a new one via the refresh token...");
            var refreshResponse = await http.PostAsJsonAsync("/api/auth/refresh", new { refreshToken });
            var refreshBody = await refreshResponse.Content.ReadAsStringAsync();

            if (!refreshResponse.IsSuccessStatusCode)
            {
                Log($"Refresh failed ({(int)refreshResponse.StatusCode} {refreshResponse.StatusCode}): {refreshBody}. Stopping simulation.");
                break;
            }

            var newAuth = JsonSerializer.Deserialize<AuthResult>(refreshBody, jsonOptions)!;
            accessToken = newAuth.AccessToken;
            refreshToken = newAuth.RefreshToken;
            hasRefreshedOnce = true;
            Log("Refresh succeeded. Received a new access token + refresh token.");
            continue; // retry the service call immediately with the new token
        }

        if (hasRefreshedOnce)
        {
            callsSinceRefresh++;
            if (callsSinceRefresh >= callsToMakeAfterRefresh)
            {
                Log("Simulation complete: logged in, accessed the service, refreshed after expiry, and accessed it again.");
                break;
            }
        }

        await Task.Delay(pollInterval);
    }
}
catch (HttpRequestException ex)
{
    Log($"Lost connection to the API at {baseUrl} ({ex.Message}). Stopping simulation.");
}

// shape of the JSON returned by both /api/auth/login and /api/auth/refresh
record AuthResult(string AccessToken, string RefreshToken);
