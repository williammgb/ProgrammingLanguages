using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using MyApp.Data;
using MyApp.Data.Models;
using MyApp.Data.SeedData;

[ApiController] // marks class as controller
[Route("api/[controller]")] // defines URL route (SeedController → seed)
// creates controller that can return HTTP responses
public class SeedController : ControllerBase
{
    // this constructor runs when ASP.NET creates instance of the controller;
    // an instance of the registered AppDbContext (in Program.cs) is created and passed here
    private readonly AppDbContext db;
    private readonly IPasswordHasher<User> passwordHasher;
    public SeedController(AppDbContext database, IPasswordHasher<User> hasher)
    {
        db = database;
        passwordHasher = hasher;
    }

    // creates enpoint POST /api/seed
    [HttpPost]
    public IActionResult Seed()
    {
        db.Database.EnsureCreated();

        // skip if already seeded, so calling this twice doesn't duplicate data
        if (db.Users.Any())
        {
            return Ok("Database already seeded.");
        }

        // one Customer + one User per test account, so we know exactly who we can log in as
        foreach (var testUser in TestUsers.All)
        {
            var customer = new Customer
            {
                Id = testUser.Id,
                FirstName = testUser.Username,
                Age = testUser.Age,
                Email = testUser.Email
            };
            db.Customers.Add(customer);

            var user = new User
            {
                Id = testUser.Id,
                Username = testUser.Username,
                PasswordHash = string.Empty, // placeholder, hashed right below
                CustomerId = customer.Id
            };
            // HashPassword salts + hashes, so we never store the raw password
            user.PasswordHash = passwordHasher.HashPassword(user, testUser.Password);
            db.Users.Add(user);
        }

        db.SaveChanges();

        return Ok($"Seeded {TestUsers.All.Length} test users.");
    }
}
