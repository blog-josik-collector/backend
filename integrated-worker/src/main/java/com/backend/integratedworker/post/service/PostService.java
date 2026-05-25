package com.backend.integratedworker.post.service;

import com.backend.commondataaccess.persistence.common.enums.DocumentStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.integratedworker.post.repository.PostRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public void createPostsIfAbsent(final List<UUID> postIds) {
        Set<UUID> allById = postRepository.findAllById(postIds)
                                          .stream()
                                          .map(Post::id)
                                          .collect(Collectors.toSet());

        List<UUID> difference = new ArrayList<>(postIds);
        difference.removeAll(allById);

        List<Post> posts = new ArrayList<>();

        for (UUID postId : difference) {
            Post post = Post.builder()
                            .id(postId)
                            .status(DocumentStatus.ACTIVE)
                            .likeCount(0)
                            .viewCount(0)
                            .commentCount(0)
                            .totalReportCount(0)
                            .build();

            posts.add(post);
        }

        postRepository.saveAll(posts);
    }
}
