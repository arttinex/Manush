package com.manush.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebPageController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "People Helping People");
        return "index";
    }

    @GetMapping("/feed")
    public String feed(Model model) {
        model.addAttribute("pageTitle", "Community Feed");
        return "feed";
    }

    @GetMapping("/help")
    public String findHelp(Model model) {
        model.addAttribute("pageTitle", "Find Help");
        return "help/find";
    }

    @GetMapping("/help/ask")
    public String askHelp(Model model) {
        model.addAttribute("pageTitle", "Ask for Help");
        return "help/ask";
    }

    @GetMapping("/jobs")
    public String jobs(Model model) {
        model.addAttribute("pageTitle", "Jobs & Opportunities");
        return "jobs";
    }

    @GetMapping("/blood")
    public String bloodBank(Model model) {
        model.addAttribute("pageTitle", "Blood Bank & Donors");
        return "blood";
    }

    @GetMapping("/jogajog")
    public String jogajog(Model model) {
        model.addAttribute("pageTitle", "Jogajog Messaging");
        return "jogajog";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("pageTitle", "My Profile");
        return "profile";
    }

    @GetMapping("/community")
    public String community(Model model) {
        model.addAttribute("pageTitle", "Community Forum");
        return "community";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        model.addAttribute("pageTitle", "Leaderboard & Community Heroes");
        return "leaderboard";
    }

    @GetMapping("/ai")
    public String aiAssistant(Model model) {
        model.addAttribute("pageTitle", "Manush AI Assistant");
        return "ai";
    }

    @GetMapping("/auth/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "Sign In");
        return "auth/login";
    }

    @GetMapping("/auth/register")
    public String register(Model model) {
        model.addAttribute("pageTitle", "Join Manush");
        return "auth/register";
    }

    @GetMapping("/auth/callback")
    public String oauthCallback() {
        return "auth/callback";
    }

    @GetMapping("/auth/complete-profile")
    public String completeProfile(Model model) {
        model.addAttribute("pageTitle", "Complete Your Profile");
        return "auth/complete-profile";
    }

    @GetMapping({"/admin", "/admin/moderation"})
    public String adminModeration(Model model) {
        model.addAttribute("pageTitle", "Admin Moderation & Verification");
        return "admin/moderation";
    }
}
