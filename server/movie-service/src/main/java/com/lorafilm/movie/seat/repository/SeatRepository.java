package com.lorafilm.movie.seat.repository;

import com.lorafilm.movie.seat.domain.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    Optional<Seat> findByPublicIdAndDeletedAtIsNull(String publicId);
    
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Seat s WHERE s.auditorium.id = :auditoriumId")
    void deleteByAuditoriumId(@Param("auditoriumId") Long auditoriumId);
    
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.auditorium.id = :auditoriumId AND s.deletedAt IS NULL")
    long countByAuditoriumIdAndDeletedAtIsNull(@Param("auditoriumId") Long auditoriumId);
    
    boolean existsByAuditoriumIdAndSeatCodeAndDeletedAtIsNull(Long auditoriumId, String seatCode);
    
    boolean existsByAuditoriumIdAndPositionRowAndPositionColumnAndDeletedAtIsNull(Long auditoriumId, Integer positionRow, Integer positionColumn);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Seat s WHERE s.auditorium.id = :auditoriumId AND s.seatCode = :seatCode AND s.id != :excludeId AND s.deletedAt IS NULL")
    boolean existsByAuditoriumIdAndSeatCodeAndIdNotAndDeletedAtIsNull(@Param("auditoriumId") Long auditoriumId, @Param("seatCode") String seatCode, @Param("excludeId") Long excludeId);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Seat s WHERE s.auditorium.id = :auditoriumId AND s.positionRow = :positionRow AND s.positionColumn = :positionColumn AND s.id != :excludeId AND s.deletedAt IS NULL")
    boolean existsByAuditoriumIdAndPositionRowAndPositionColumnAndIdNotAndDeletedAtIsNull(@Param("auditoriumId") Long auditoriumId, @Param("positionRow") Integer positionRow, @Param("positionColumn") Integer positionColumn, @Param("excludeId") Long excludeId);
    
    boolean existsBySeatTypeIdAndDeletedAtIsNull(Long seatTypeId);
    
    @Query("SELECT s.seatCode as seatCode, s.positionRow as positionRow, s.positionColumn as positionColumn FROM Seat s WHERE s.auditorium.id = :auditoriumId AND s.deletedAt IS NULL")
    List<SeatConflictProjection> findConflictDataByAuditoriumId(@Param("auditoriumId") Long auditoriumId);

    @Query("SELECT s FROM Seat s JOIN FETCH s.seatType st " +
           "WHERE s.auditorium.id = :auditoriumId " +
           "AND s.deletedAt IS NULL " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY s.positionRow ASC, s.positionColumn ASC")
    List<Seat> findAdminLayoutByAuditoriumId(@Param("auditoriumId") Long auditoriumId);

    @Query("SELECT s FROM Seat s JOIN FETCH s.seatType st " +
           "WHERE s.auditorium.id = :auditoriumId " +
           "AND s.deletedAt IS NULL AND s.status IN ('ACTIVE', 'MAINTENANCE') " +
           "AND st.deletedAt IS NULL AND st.status = 'ACTIVE' " +
           "ORDER BY s.positionRow ASC, s.positionColumn ASC")
    List<Seat> findCustomerLayoutByAuditoriumId(@Param("auditoriumId") Long auditoriumId);

    @Query("SELECT DISTINCT st.publicId FROM Seat s JOIN s.seatType st " +
           "WHERE s.auditorium.id = :auditoriumId " +
           "AND s.deletedAt IS NULL AND s.status IN ('ACTIVE', 'MAINTENANCE') " +
           "AND st.deletedAt IS NULL AND st.status = 'ACTIVE'")
    List<String> findActiveSeatTypePublicIdsByAuditoriumId(@Param("auditoriumId") Long auditoriumId);

    @Query("SELECT DISTINCT st FROM Seat s JOIN s.seatType st " +
           "WHERE s.auditorium.id = :auditoriumId " +
           "AND s.deletedAt IS NULL AND s.status IN ('ACTIVE', 'MAINTENANCE') " +
           "AND st.deletedAt IS NULL AND st.status = 'ACTIVE' " +
           "ORDER BY st.publicId ASC")
    List<com.lorafilm.movie.seat.domain.entity.SeatType> findActiveSeatTypesByAuditoriumId(
            @Param("auditoriumId") Long auditoriumId);

    @Query("SELECT DISTINCT s.auditorium.id, st FROM Seat s JOIN s.seatType st " +
           "WHERE s.auditorium.id IN :auditoriumIds " +
           "AND s.deletedAt IS NULL AND s.status IN ('ACTIVE', 'MAINTENANCE') " +
           "AND st.deletedAt IS NULL AND st.status = 'ACTIVE' " +
           "ORDER BY s.auditorium.id ASC, st.publicId ASC")
    List<Object[]> findActiveSeatTypesByAuditoriumIds(
            @Param("auditoriumIds") List<Long> auditoriumIds);

    @Query("SELECT s.auditorium.id, st.id, COUNT(s) FROM Seat s JOIN s.seatType st " +
           "WHERE s.auditorium.id IN :auditoriumIds " +
           "AND s.deletedAt IS NULL AND s.status IN ('ACTIVE', 'MAINTENANCE') " +
           "AND st.deletedAt IS NULL AND st.status = 'ACTIVE' " +
           "GROUP BY s.auditorium.id, st.id")
    List<Object[]> countActiveSeatsByAuditoriumAndSeatType(
            @Param("auditoriumIds") List<Long> auditoriumIds);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"seatType"})
    List<Seat> findByAuditoriumIdAndDeletedAtIsNull(Long auditoriumId);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"seatType"})
    List<Seat> findByIdInAndDeletedAtIsNull(List<Long> ids);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"seatType"})
    List<Seat> findByPublicIdInAndDeletedAtIsNull(List<String> publicIds);
}
