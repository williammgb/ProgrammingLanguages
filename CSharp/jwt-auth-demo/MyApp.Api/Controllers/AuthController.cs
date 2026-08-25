using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using MyApp.Api.Dtos;
using MyApp.Api.Services;
using MyApp.Data;
using MyApp.Data.Models;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly AppDbContext db;
    private readonly IPasswordHasher<User> passwordHasher;
    private readonly ITokenService tokenService;

    public AuthController(AppDbContext database, IPasswordHasher<User> hasher, ITokenService tokens)
    {
        db = database;
        passwordHasher = hasher;
        tokenService = tokens;
    }

    // POST /api/auth/login: check credentials, hand back a fresh token pair
    [HttpPost("login")]
    public IActionResult Login(LoginRequest request)
    {
        var user = db.Users.FirstOrDefault(u => u.Username == request.Username);
        if (user is null)
        {
            return Unauthorized("Invalid username or password.");
        }

        // compares the raw password against the stored hash, no need to un-hash anything
        var result = passwordHasher.VerifyHashedPassword(user, user.PasswordHash, request.Password);
        if (result == PasswordVerificationResult.Failed)
        {
            return Unauthorized("Invalid username or password.");
        }

        var accessToken = tokenService.CreateAccessToken(user);
        var refreshToken = tokenService.CreateRefreshToken(user.Id);
        db.RefreshTokens.Add(refreshToken);
        db.SaveChanges();

        return Ok(new AuthResponse(accessToken, refreshToken.Token));
    }

    // POST /api/auth/refresh: trade a valid refresh token for a new token pair
    [HttpPost("refresh")]
    public IActionResult Refresh(RefreshRequest request)
    {
        var existing = db.RefreshTokens
            .Include(r => r.User)
            .FirstOrDefault(r => r.Token == request.RefreshToken);

        if (existing is null || existing.RevokedAt is not null || existing.ExpiresAt < DateTime.UtcNow)
        {
            return Unauthorized("Invalid or expired refresh token.");
        }

        // rotate: kill the old refresh token, issue a brand new pair
        existing.RevokedAt = DateTime.UtcNow;

        var accessToken = tokenService.CreateAccessToken(existing.User!);
        var newRefreshToken = tokenService.CreateRefreshToken(existing.UserId);
        db.RefreshTokens.Add(newRefreshToken);
        db.SaveChanges();

        return Ok(new AuthResponse(accessToken, newRefreshToken.Token));
    }

    // POST /api/auth/logout: revoke the refresh token so it can't be used again
    [HttpPost("logout")]
    public IActionResult Logout(RefreshRequest request)
    {
        var existing = db.RefreshTokens.FirstOrDefault(r => r.Token == request.RefreshToken);
        if (existing is not null && existing.RevokedAt is null)
        {
            existing.RevokedAt = DateTime.UtcNow;
            db.SaveChanges();
        }

        return Ok("Logged out.");
    }
}
