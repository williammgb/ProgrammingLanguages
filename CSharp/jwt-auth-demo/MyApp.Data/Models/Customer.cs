namespace MyApp.Data.Models;

public class Customer
{
    // properties to get (obj.var) and set (obj.var = val) the value
    // required means variable cannot be 0
    public required int Id { get; set; }
    public required string FirstName { get; set; }
    public required int Age  { get; set; }
    public required string Email  { get; set; }
}