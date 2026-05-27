package com.backend.interactionservice.postbookmark.repository;

import com.backend.commondataaccess.persistence.post.PostBookmark;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostBookmarkRepository extends JpaRepository<PostBookmark, UUID> {

}
