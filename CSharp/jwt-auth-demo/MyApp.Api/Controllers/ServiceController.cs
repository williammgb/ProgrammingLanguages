using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

// the protected resource: needs a valid access token to reach it
[ApiController]
[Route("api/[controller]")]
[Authorize]
public class ServiceController : ControllerBase
{
    // GET /api/service: only reachable with a valid, non-expired access token
    [HttpGet]
    public IActionResult Access()
    {
        var username = User.FindFirstValue(ClaimTypes.Name) ?? User.Identity?.Name;
        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);

        return Ok(new
        {
            message = $"Hello {username}, access granted.",
            userId,
            serverTimeUtc = DateTime.UtcNow
        });
    }
}
