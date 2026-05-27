package com.backend.interactionservice.postlike.repository;

import com.backend.commondataaccess.persistence.post.PostLike;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

}
