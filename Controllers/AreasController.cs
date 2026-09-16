using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using FixITCityAPI.Models;

namespace FixITCityAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AreasController : ControllerBase
{
    private readonly FixITCityDbContext _context;

    public AreasController(FixITCityDbContext context)
    {
        _context = context;
    }

    [HttpGet]
    public async Task<IActionResult> GetAllAreas()
    {
        try
        {
            var areas = await _context.ServiceAreas
                .Where(a => a.IsActive == true)
                .OrderBy(a => a.AreaName)
                .ToListAsync();

            return Ok(areas);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    [HttpGet("{id}")]
    public async Task<IActionResult> GetArea(int id)
    {
        try
        {
            var area = await _context.ServiceAreas.FindAsync(id);
            if (area == null)
                return NotFound(new { message = "Area not found" });

            return Ok(area);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    [HttpGet("essential-services")]
    public async Task<IActionResult> GetEssentialServices()
    {
        try
        {
            var services = await _context.EssentialServices
                .OrderBy(s => s.Name)
                .ToListAsync();

            return Ok(services);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }
}