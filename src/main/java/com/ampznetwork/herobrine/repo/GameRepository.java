package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.games.model.Game;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface GameRepository extends CrudRepository<Game, String> {
    @Query("""
            select game from Game game
             join GuildGameFlaglist flags on flags.guildId = :guildId
             inner join flags.games anyGame on (flags.type = 1 and anyGame.name = game.name)
                                            or (flags.type = 0 and anyGame.name != game.name)
            """)
    Collection<Game> findAllByGuildId(long guildId);
}
