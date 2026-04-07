package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.accountlink.model.entity.LinkedAccount;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;
import java.util.UUID;

@Controller
@Repository
@RequestMapping("/linked_account")
public interface LinkedAccountRepository extends CrudRepository<LinkedAccount, @NotNull Long> {
    @NonNull
    @Override
    @ResponseBody
    @GetMapping("/by_discord/{id}")
    Optional<LinkedAccount> findById(@PathVariable("id") @NotNull Long aLong);

    @ResponseBody
    @GetMapping("/by_minecraft/{id}")
    Optional<LinkedAccount> findByMinecraftId(@PathVariable("id") UUID minecraftId);
}
