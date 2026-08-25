namespace MyApp.Api.Services;

using MyApp.Data.Models;

// creates the tokens a client needs after logging in
public interface ITokenService
{
    // short-lived signed JWT sent on every authenticated request
    string CreateAccessToken(User user);

    // long-lived opaque token, stored in DB, used to get a new access token later
    RefreshToken CreateRefreshToken(int userId);
}
