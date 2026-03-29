package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.component.permission.model.RolePermissionAssignment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionAssignmentRepository extends CrudRepository<RolePermissionAssignment, RolePermissionAssignment.Key> {}
