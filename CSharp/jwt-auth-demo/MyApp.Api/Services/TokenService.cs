namespace MyApp.Api.Services;

using System.Globalization;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using Microsoft.IdentityModel.Tokens;
using MyApp.Data.Models;

public class TokenService : ITokenService
{
    // IConfiguration gives access to appsettings.json (the Jwt section)
    private readonly IConfiguration config;
    public TokenService(IConfiguration configuration)
    {
        config = configuration;
    }

    public string CreateAccessToken(User user)
    {
        var jwtSection = config.GetSection("Jwt");
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSection["Key"]!));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        // Claims are pieces of information about the user stored inside the JWT
        var claims = new[]
        {
            new Claim(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
            new Claim(JwtRegisteredClaimNames.UniqueName, user.Username)
        };

        // InvariantCulture: config values are always "."-decimal regardless of the host OS locale
        // (on a locale where "." is a group separator, plain double.Parse("0.5") silently returns 5)
        var minutes = double.Parse(jwtSection["AccessTokenMinutes"]!, CultureInfo.InvariantCulture);
        var token = new JwtSecurityToken(
            issuer: jwtSection["Issuer"],
            audience: jwtSection["Audience"],
            claims: claims,
            expires: DateTime.UtcNow.AddMinutes(minutes),
            signingCredentials: credentials
        );

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    public RefreshToken CreateRefreshToken(int userId)
    {
        var days = double.Parse(config["Jwt:RefreshTokenDays"]!, CultureInfo.InvariantCulture);

        // random bytes instead of a JWT here, it just needs to be unguessable
        return new RefreshToken
        {
            Id = 0, // 0 = let the database assign the real id
            Token = Convert.ToBase64String(RandomNumberGenerator.GetBytes(64)),
            CreatedAt = DateTime.UtcNow,
            ExpiresAt = DateTime.UtcNow.AddDays(days),
            UserId = userId
        };
    }
}
