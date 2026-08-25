using System.Text;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore; // ensure this is referenced in .csproj
using Microsoft.IdentityModel.Tokens;
using MyApp.Api.Services;
using MyApp.Data; // ensure this is referenced in .csproj
using MyApp.Data.Models;

// Creates ASP.NET app setup (loading config, preparing dependencies/loggins/webserver)
var builder = WebApplication.CreateBuilder(args);

// Whenever controller needs AppDbContext, create new one every time and give it.
// Connection string comes from appsettings.json (ConnectionStrings:Default) instead
// of being hardcoded, so it can be changed per environment without touching code.
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(builder.Configuration.GetConnectionString("Default")));

// hashes/verifies passwords; registered once (singleton) so controllers can just ask for it
// IPasswordHasher is template/contract; PasswordHasher is implementation. Both are built-in
builder.Services.AddSingleton<IPasswordHasher<User>, PasswordHasher<User>>();

// issues JWT access tokens + refresh tokens after a successful login
builder.Services.AddSingleton<ITokenService, TokenService>();

// reads the Jwt section from appsettings.json (Key/Issuer/Audience)
var jwtSection = builder.Configuration.GetSection("Jwt");
var jwtKey = jwtSection["Key"]!;

// tells ASP.NET how to validate a JWT sent in the Authorization header
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = jwtSection["Issuer"],
            ValidateAudience = true,
            ValidAudience = jwtSection["Audience"],
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey)),
            ValidateLifetime = true,
            ClockSkew = TimeSpan.Zero // default is a 5-min grace period past expiry; too generous for short-lived tokens
        };
    });
builder.Services.AddAuthorization();

// Register controllers: tell ASP.NET this API has controllers with HTTP enpoints
builder.Services.AddControllers();

// Build actual web app
var app = builder.Build();

// Fail fast with a clear message instead of letting every request blow up with a
// raw Npgsql stack trace when Postgres (e.g. the docker container) isn't running.
using (var startupScope = app.Services.CreateScope())
{
    var db = startupScope.ServiceProvider.GetRequiredService<AppDbContext>();
    try
    {
        if (!db.Database.CanConnect())
        {
            Console.Error.WriteLine("Could not connect to the database. Is Postgres running? (e.g. `docker start postgres-db`)");
            Environment.Exit(1);
        }
    }
    catch (Exception ex)
    {
        Console.Error.WriteLine($"Could not connect to the database. Is Postgres running? (e.g. `docker start postgres-db`)\n{ex.Message}");
        Environment.Exit(1);
    }
}

// Catch anything unhandled (like a DB call failing mid-request) and return a
// generic JSON error instead of leaking a stack trace to the client.
// In other words: global exception handling
app.UseExceptionHandler(errorApp =>
{
    // Means: run this code when an exception occurs
    errorApp.Run(async context =>
    {
        // Gets information about the exception that occurred
        var feature = context.Features.Get<Microsoft.AspNetCore.Diagnostics.IExceptionHandlerFeature>();
        // Logs the actual error (logs to ASP.NET Core's configured logging providers)
        app.Logger.LogError(feature?.Error, "Unhandled exception");

        // Creates a JSON response with error-code 500
        // This is what the client sees
        context.Response.StatusCode = StatusCodes.Status500InternalServerError;
        context.Response.ContentType = "application/json";
        await context.Response.WriteAsJsonAsync(new { error = "An unexpected error occurred." });
    });
});

// must come before MapControllers so [Authorize] endpoints actually check the token
app.UseAuthentication();
app.UseAuthorization();

// Connects HTTP requests to controllers (e.g., POST /api/seed → SeedController.Seed())
app.MapControllers();

app.Run();
