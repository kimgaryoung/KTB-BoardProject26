package com.example.boardproject.repository;

import com.example.boardproject.entity.Viewer;
import com.example.boardproject.entity.ViewerId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViewerRepository extends JpaRepository<Viewer, ViewerId> {

    //existsByPostIdAndUserId


}
