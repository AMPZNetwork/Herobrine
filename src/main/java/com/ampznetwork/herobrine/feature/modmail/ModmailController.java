package com.ampznetwork.herobrine.feature.modmail;

import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/modmail")
public class ModmailController {
    @Autowired JDA jda;

    @GetMapping
    public String get() {
        return "modmail";
    }

    @PostMapping
    public void post(@RequestParam("content") String content) {
        jda.openPrivateChannelById(141476933849448448L).flatMap(channel -> channel.sendMessage(content)).queue();
    }
}
