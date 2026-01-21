package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByVideoIdAndParentCommentIsNullOrderByCreatedAtDesc(Long videoId);

    @Modifying
    @Query("delete from Comment c where c.video.id = :videoId")
    void deleteByVideoId(@Param("videoId") Long videoId);
}
