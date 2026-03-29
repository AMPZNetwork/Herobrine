package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.component.permission.model.UserPermissionAssignment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPermissionAssignmentRepository extends CrudRepository<UserPermissionAssignment, UserPermissionAssignment.Key> {
}
