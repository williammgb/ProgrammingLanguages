using Microsoft.AspNetCore.Mvc;
using MyApp.Data;

// plain data-query endpoint, no auth required; ServiceController is the protected one
[ApiController]
[Route("api/[controller]")]
public class TestController : ControllerBase
{
    // add instance of the database-context via constructor
    private readonly AppDbContext db;
    public TestController(AppDbContext database)
    {
        db = database;
    }

    [HttpGet]
    public IActionResult Query()
    {
        // get all customers
        var allCustomers = db.Customers.ToList();
        Console.WriteLine($"Total customers: {allCustomers.Count}");
        // FirstOrDefault instead of a fixed index, so this doesn't break on a small seed
        var exampleCustomer = allCustomers.FirstOrDefault();
        Console.WriteLine($"Example customer: {exampleCustomer?.FirstName}");

        // get customer by ID (first seeded customer's ID, instead of a hardcoded one)
        var customer = exampleCustomer is null
            ? null
            : db.Customers.FirstOrDefault(c => c.Id == exampleCustomer.Id);

        Console.WriteLine($"Customer found: {customer?.Email}");

        // filter customers by age
        var elders = db.Customers
            .Where(c => c.Age >= 65)
            .ToList();
        Console.WriteLine($"There are {elders.Count} elders.");

        // creates an HTTP 200 OK response and sends the object inside as JSON
        return Ok(new
        {
            totalCustomers = allCustomers.Count,
            exampleCustomer,
            elders = elders.Count
        });
    }
}