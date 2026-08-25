namespace MyApp.Api.Dtos;

// what the client sends to /api/auth/login
public record LoginRequest(string Username, string Password);

// what the client sends to /api/auth/refresh and /api/auth/logout
public record RefreshRequest(string RefreshToken);

// what we send back after a successful login or refresh
public record AuthResponse(string AccessToken, string RefreshToken);
