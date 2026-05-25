package com.backend.integratedworker.post.repository;

import com.backend.commondataaccess.persistence.post.Post;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

}
