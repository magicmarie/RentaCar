package com.rentacar.repository;

import com.rentacar.entity.Reservation;
import com.rentacar.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByCustomerIdOrderByStartDateDesc(Long customerId);

    @Query("""
            select r from Reservation r
            where r.vehicle.id = :vehicleId
              and r.status in :statuses
              and r.startDate <= :endDate
              and r.endDate >= :startDate
            """)
    List<Reservation> findOverlapping(@Param("vehicleId") Long vehicleId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("statuses") Collection<ReservationStatus> statuses);

    boolean existsByVehicleIdAndStatusIn(Long vehicleId, Collection<ReservationStatus> statuses);

    List<Reservation> findByStatus(ReservationStatus status);

    @Query("select r from Reservation r where r.status in :statuses and r.startDate >= :fromDate order by r.startDate asc")
    List<Reservation> findUpcoming(@Param("statuses") Collection<ReservationStatus> statuses,
                                    @Param("fromDate") LocalDate fromDate);

    @Query("""
            select r from Reservation r
            where (:status is null or r.status = :status)
              and (:search is null
                   or lower(r.customer.firstName) like :search
                   or lower(r.customer.lastName) like :search
                   or lower(r.customer.email) like :search
                   or lower(r.vehicle.licensePlate) like :search)
            order by r.startDate desc
            """)
    List<Reservation> search(@Param("status") ReservationStatus status, @Param("search") String search);
}
