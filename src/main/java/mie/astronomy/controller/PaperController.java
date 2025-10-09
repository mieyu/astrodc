package mie.astronomy.controller;

import mie.astronomy.common.Result;
import mie.astronomy.entity.Paper;
import mie.astronomy.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/paper")
public class PaperController {

    @Autowired
    private PaperService paperService;

    @GetMapping("/galaxies")
    public Result<List<String>> getGalaxies() {
        List<String> galaxies = paperService.getGalaxies();
        return Result.success(galaxies);
    }

    @GetMapping("/by-galaxy")
    public Result<List<Paper>> getPapersByGalaxy(@RequestParam String galaxy) {
        List<Paper> papers = paperService.getPapersByGalaxy(galaxy);
        return Result.success(papers);
    }
}