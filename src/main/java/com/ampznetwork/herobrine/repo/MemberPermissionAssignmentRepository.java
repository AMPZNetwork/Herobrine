package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.component.user.permission.model.MemberPermissionAssignment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberPermissionAssignmentRepository extends CrudRepository<MemberPermissionAssignment, MemberPermissionAssignment.Key> {
}
