package com.backend.interactionservice.postcomment.repository;

import com.backend.commondataaccess.persistence.post.PostComment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {

}
