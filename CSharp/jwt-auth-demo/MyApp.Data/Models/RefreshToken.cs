namespace MyApp.Data.Models;

public class RefreshToken
{
    public required int Id { get; set; }

    public required string Token { get; set; }

    public required DateTime CreatedAt { get; set; }

    public required DateTime ExpiresAt { get; set; }

    public DateTime? RevokedAt { get; set; }

    public required int UserId { get; set; }
    public User? User { get; set; }
}