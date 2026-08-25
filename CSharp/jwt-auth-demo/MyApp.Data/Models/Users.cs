namespace MyApp.Data.Models;

public class User
{
    public required int Id { get; set; }

    public required string Username { get; set; }

    public required string PasswordHash { get; set; }

    // EFC convention: <name>Id followed by <name> with type <name>
    // means: User.<name>Id --> <name>.Id (it knows it is a foreign key);
    // Customer is not stored in table, just used to define relationship;
    // Can be used to automatically join tables
    public required int CustomerId { get; set; }
    public Customer? Customer { get; set; } // ? means it can be null

    public List<RefreshToken> RefreshTokens { get; set; } = [];
}