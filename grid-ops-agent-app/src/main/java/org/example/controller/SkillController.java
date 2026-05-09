package org.example.controller;

import org.example.agent.skill.model.Skill;
import org.example.agent.skill.service.SkillRegistry;
import org.example.agent.skill.service.SkillSelector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    @Autowired
    private SkillRegistry skillRegistry;

    @Autowired
    private SkillSelector skillSelector;

    @GetMapping
    public Map<String, Object> listSkills(
            @RequestParam(value = "category", required = false) String category) {
        List<Skill> skills = category != null
                ? skillRegistry.getSkillsByCategory(category)
                : skillRegistry.getEnabledSkills();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("skills", skills);
        response.put("total", skills.size());
        return response;
    }

    @GetMapping("/{skillId}")
    public Map<String, Object> getSkill(@PathVariable String skillId) {
        Optional<Skill> skill = skillRegistry.getSkill(skillId);
        Map<String, Object> response = new LinkedHashMap<>();
        if (skill.isPresent()) {
            response.put("skill", skill.get());
        } else {
            response.put("error", "Skill不存在: " + skillId);
        }
        return response;
    }

    @PostMapping("/select")
    public Map<String, Object> selectSkill(@RequestBody Map<String, String> request) {
        String alarmType = request.get("alarmType");
        String intent = request.get("intent");

        Optional<Skill> selected = alarmType != null
                ? skillSelector.selectByAlarmType(alarmType)
                : skillSelector.selectByIntent(intent != null ? intent : "");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("selected", selected.orElse(null));
        return response;
    }
}
