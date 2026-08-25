namespace MyApp.Data.SeedData;

// one entry = one Customer + one User we can log in as while building auth
public record TestUser(int Id, string Username, string Password, string Email, int Age);

public static class TestUsers
{
    public static readonly TestUser[] All =
    {
        new(1, "alice", "Password123!", "alice@domain.com", 30),
        new(2, "bob", "Password123!", "bob@domain.com", 25),
        new(3, "charlie", "Password123!", "charlie@domain.com", 40),
    };
}
