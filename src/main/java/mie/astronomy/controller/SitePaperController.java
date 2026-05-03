package mie.astronomy.controller;

import mie.astronomy.common.Result;
import mie.astronomy.entity.SitePaper;
import mie.astronomy.service.SitePaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/site/paper")
public class SitePaperController {

    @Autowired
    private SitePaperService sitePaperService;

    @GetMapping("/galaxies")
    public Result<List<String>> getGalaxies() {
        List<String> galaxies = sitePaperService.getGalaxies();
        return Result.success(galaxies);
    }

    @GetMapping("/by-galaxy")
    public Result<List<SitePaper>> getPapersByGalaxy(@RequestParam String galaxy) {
        List<SitePaper> papers = sitePaperService.getPapersByGalaxy(galaxy);
        return Result.success(papers);
    }
}
