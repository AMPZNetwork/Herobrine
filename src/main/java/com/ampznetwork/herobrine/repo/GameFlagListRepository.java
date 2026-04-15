package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.component.games.model.GameFlaglist;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameFlagListRepository extends CrudRepository<GameFlaglist, GameFlaglist.Key> {
}
