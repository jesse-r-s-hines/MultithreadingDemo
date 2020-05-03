using System;
using Microsoft.AspNetCore.Mvc;
using Server.Models;
using System.Text.Encodings.Web;
using System.Linq;
using System.Collections.Generic;
using Microsoft.EntityFrameworkCore;
using System.Text.Json;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using System.Security.Claims;
using Helpers;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc.Rendering;

namespace Server.Controllers
{
    public class MultithreadingController : Controller
    {
        private readonly MultithreadingContext context;
        private readonly UserManager<IdentityUser> userManager;

        public MultithreadingController(MultithreadingContext context, UserManager<IdentityUser> userManager)
        {
            this.context = context;
            this.userManager = userManager;
        }

        public async Task<IActionResult> Index(string filter, int? userPage, int? publicPage)
        {
            ViewData["Filter"] = filter;

            var userId = this.userManager.GetUserId(this.User);

            IQueryable<Demo> query = context.Demo
                .Select(s => s)
                .Include(d => d.Author)
                .Include(d => d.DemoTag)
                .Where(d => d.AuthorId == userId || d.Visibility == Models.Demo.PUBLIC)
                .OrderByDescending(d => d.UpdatedAt);

            if (!String.IsNullOrEmpty(filter))
                query = query.Where( d =>
                    d.Title.ToLower().Contains(filter.ToLower()) ||
                    d.Description.ToLower().Contains(filter.ToLower()) ||
                    d.DemoTag.Any(dt => dt.TagName.ToLower().Contains(filter.ToLower())) );
    
            if (userId != null) {
                var userDemosQ = query.Where(d => d.AuthorId == userId);
                var publicDemosQ = query.Where(d => d.AuthorId != userId);

                var userDemos = await PaginatedList<Demo>.CreateAsync(userDemosQ, userPage ?? 1, 10);
                var publicDemos = await PaginatedList<Demo>.CreateAsync(publicDemosQ, publicPage ?? 1, 10);

                return View("Index", (userDemos, publicDemos) );
            } else {
                var publicDemos = await PaginatedList<Demo>.CreateAsync(query, publicPage ?? 1, 10);

                return View("Index", ( (PaginatedList<Demo>) null, publicDemos) );

            }

        }

        public IActionResult Help() {
            return View("Help");
        }
        
        public IActionResult Demo(long? id)
        {
            ViewBag.Tags = context.Tag.Select( t => new SelectListItem(t.Name, t.Name) ).ToList();

            if (id == null) {
                return View("Demo", new Demo());
            } else {
                if (!context.Demo.Any(d => d.Id == id)) return NotFound();
                var demo = context.Demo
                    .Include(d => d.Thread)
                    .Include(d => d.Playback)
                    .ThenInclude(p => p.Step)
                    .Include(d => d.DemoTag)
                    .Where(d => d.Id == id)
                    .Single();

                var userId = this.userManager.GetUserId(this.User);
                if (demo.AuthorId != userId && demo.Visibility == Models.Demo.PRIVATE) return Forbid();

                foreach (var item in ViewBag.Tags) {
                    item.Selected = (demo.DemoTag.Any(t => t.TagName == item.Value));
                }

                return View("Demo", demo);
            }
        }

        [HttpPost]
        [Authorize] // require logged in user.
        public IActionResult Save([Bind("Id,Title,Visibility,Description,DemoTag,Playback,Thread")] Demo demo)  
        {  
            if (ModelState.IsValid) {
                var userId = this.userManager.GetUserId(this.User);

                if ( demo.Id == 0 ) { // No ID, create a new demo.
                    demo.AuthorId = userId;
                    demo.CreatedAt = DateTime.Now;
                    demo.UpdatedAt = DateTime.Now;

                    context.Demo.Add(demo);
                } else {
                    var dbDemo = context.Demo
                        .Include(d => d.Thread)
                        .Include(d => d.Playback)
                        .ThenInclude(p => p.Step)
                        .Include(d => d.DemoTag)
                        // .ThenInclude(p => p.TagNameNavigation)
                        .Where(d => d.Id == demo.Id)
                        .Single();

                    if (dbDemo == null) return BadRequest("Model not found.");

                    if (dbDemo.AuthorId == userId) { // update demo
                        dbDemo.Title = demo.Title;
                        dbDemo.Visibility = demo.Visibility;
                        dbDemo.Description = demo.Description;
                        dbDemo.DemoTag = demo.DemoTag;
                        dbDemo.Playback = demo.Playback;
                        dbDemo.Thread = demo.Thread;

                        demo.UpdatedAt = DateTime.Now;

                        context.Update(dbDemo);
                    } else if (dbDemo.Visibility == Models.Demo.PUBLIC) { // Save a copy
                        demo.Id = 0;
                        foreach (var thread in demo.Thread) thread.Id = 0;

                        demo.AuthorId = userId;
                        demo.CreatedAt = DateTime.Now;
                        demo.UpdatedAt = DateTime.Now;

                        context.Demo.Add(demo);
                    } else {
                        return Forbid();
                    } 
                }

                context.SaveChanges();

                var response = new Dictionary<String, Object>();
                response.Add("success", true);
                response.Add("id", demo.Id);
                return Json(response);  
            } else {
                return BadRequest("Model invalid.");
            }
        }

        // [HttpPost]
        // [ValidateAntiForgeryToken]
        public IActionResult Delete(long id)
        {
            var demo = context.Demo.Find(id);

            var userId = this.userManager.GetUserId(this.User);
            if (demo.AuthorId != userId) return Forbid();

            context.Demo.Remove(demo);
            context.SaveChanges();
            return RedirectToAction("Index");
        }
    }
}