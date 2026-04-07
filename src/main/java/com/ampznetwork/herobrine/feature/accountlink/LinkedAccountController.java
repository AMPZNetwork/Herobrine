package com.ampznetwork.herobrine.feature.accountlink;

import com.ampznetwork.herobrine.feature.accountlink.model.entity.LinkedAccount;
import com.ampznetwork.herobrine.repo.LinkedAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Controller
@RequestMapping("/linked_account")
public class LinkedAccountController {
    @Autowired LinkedAccountRepository accounts;

    @ResponseBody
    @GetMapping("/by_discord/{id}")
    public LinkedAccount byDiscordId(@PathVariable long id) {
        return accounts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @ResponseBody
    @GetMapping("/by_minecraft/{id}")
    public LinkedAccount byMinecraftId(@PathVariable UUID id) {
        return accounts.findByMinecraftId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @ResponseBody
    @GetMapping("/by_hytale/{id}")
    public LinkedAccount byHytaleId(@PathVariable UUID id) {
        return accounts.findByHytaleId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
