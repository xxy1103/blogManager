package com.ulna.blog_manager.repository;

import com.ulna.blog_manager.model.BlogEntity;
import com.ulna.blog_manager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<BlogEntity, Long> {
    List<BlogEntity> findByUser(User user);
    List<BlogEntity> findByUserAndTitleContaining(User user, String title);
    List<BlogEntity> findByUserAndCategoriesContaining(User user, String category);
    List<BlogEntity> findByUserAndTagsContaining(User user, String tags);
    List<BlogEntity> findByUserOrderByCreatedAtDesc(User user);
    Optional<BlogEntity> findByIdAndUser(Long id, User user);
}
