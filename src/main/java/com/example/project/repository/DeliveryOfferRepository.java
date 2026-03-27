package com.example.project.repository;

import com.example.project.entity.DeliveryOffer;
import com.example.project.entity.DeliveryOfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryOfferRepository extends JpaRepository<DeliveryOffer, Long> {

    boolean existsByExchangeRequest_Id(Long exchangeRequestId);

    Optional<DeliveryOffer> findByExchangeRequest_Id(Long exchangeRequestId);

    @Query("""
        select d
        from DeliveryOffer d
        join fetch d.exchangeRequest er
        join fetch er.requesterOffer ro
        join fetch ro.user ru
        join fetch er.targetOffer toff
        join fetch toff.user tu
        left join fetch d.assignedDeliveryPartner adp
        where d.exchangeRequest.id = :exchangeRequestId
        """)
    Optional<DeliveryOffer> findByExchangeRequestIdWithDetails(@Param("exchangeRequestId") Long exchangeRequestId);

    @Query("""
        select d
        from DeliveryOffer d
        join fetch d.exchangeRequest er
        join fetch er.requesterOffer ro
        join fetch ro.user ru
        join fetch er.targetOffer toff
        join fetch toff.user tu
        left join fetch d.assignedDeliveryPartner adp
        where d.status = :status
        order by d.createdAt desc
        """)
    List<DeliveryOffer> findByStatusWithDetails(@Param("status") DeliveryOfferStatus status);

    @Query("""
        select d
        from DeliveryOffer d
        join fetch d.exchangeRequest er
        join fetch er.requesterOffer ro
        join fetch ro.user ru
        join fetch er.targetOffer toff
        join fetch toff.user tu
        left join fetch d.assignedDeliveryPartner adp
        where d.assignedDeliveryPartner.id = :deliveryPartnerId and d.status = :status
        order by d.acceptedAt desc, d.createdAt desc
        """)
    List<DeliveryOffer> findByAssigneeAndStatusWithDetails(
        @Param("deliveryPartnerId") Long deliveryPartnerId,
        @Param("status") DeliveryOfferStatus status
    );

    @Query("""
        select d
        from DeliveryOffer d
        join fetch d.exchangeRequest er
        join fetch er.requesterOffer ro
        join fetch ro.user ru
        join fetch er.targetOffer toff
        join fetch toff.user tu
        left join fetch d.assignedDeliveryPartner adp
        where d.id = :id
        """)
    Optional<DeliveryOffer> findByIdWithDetails(@Param("id") Long id);
}