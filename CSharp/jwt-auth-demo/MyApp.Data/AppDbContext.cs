namespace MyApp.Data;

using Microsoft.EntityFrameworkCore; // ensure this is referenced in .csproj
using MyApp.Data.Models; // ensure namespaces are set in files in /Models

// define bridge between C# code and the database (tools to interact with DB)
// inherits from DbContext (EFC class that provides DB functionality)
public class AppDbContext : DbContext
{   
    // define the tables which exist (class → table)
    // database table called Customers with Customer objects
    // Set<Type>() returns the DbSet<Type> (database object) via EF Core
    public DbSet<Customer> Customers => Set<Customer>();
    public DbSet<User> Users => Set<User>();
    public DbSet<RefreshToken> RefreshTokens => Set<RefreshToken>();

    // options (incl. the Npgsql connection string) now come from DI, configured in
    // Program.cs from appsettings.json, instead of being hardcoded here
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }
}
