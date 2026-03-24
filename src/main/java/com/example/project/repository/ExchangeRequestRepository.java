package com.example.project.repository;

import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.ExchangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {
    boolean existsByRequesterOffer_IdAndTargetOffer_Id(Long requesterOfferId, Long targetOfferId);

    @Query("""
        select er
        from ExchangeRequest er
        join fetch er.requesterOffer ro
        join fetch ro.user ru
        join fetch ro.book rb
        join fetch er.targetOffer toff
        join fetch toff.user tu
        join fetch toff.book tb
        order by er.createdAt desc
        """)
    List<ExchangeRequest> findAllWithDetails();

    @Query("""
        select er
        from ExchangeRequest er
        join fetch er.requesterOffer ro
        join fetch ro.user ru
        join fetch ro.book rb
        join fetch er.targetOffer toff
        join fetch toff.user tu
        join fetch toff.book tb
        where ro.user.id = :userId
        order by er.createdAt desc
        """)
    List<ExchangeRequest> findSentByUserIdWithDetails(@Param("userId") Long userId);

    @Query("""
        select er
        from ExchangeRequest er
        join fetch er.requesterOffer ro
        join fetch ro.user ru
        join fetch ro.book rb
        join fetch er.targetOffer toff
        join fetch toff.user tu
        join fetch toff.book tb
        where toff.user.id = :userId
        order by er.createdAt desc
        """)
    List<ExchangeRequest> findReceivedByUserIdWithDetails(@Param("userId") Long userId);

    @Query("""
        select er
        from ExchangeRequest er
        join fetch er.requesterOffer ro
        join fetch ro.user ru
        join fetch ro.book rb
        join fetch er.targetOffer toff
        join fetch toff.user tu
        join fetch toff.book tb
        where er.id = :id
        """)
    Optional<ExchangeRequest> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        select er
        from ExchangeRequest er
        join fetch er.requesterOffer ro
        join fetch ro.user ru
        join fetch ro.book rb
        join fetch er.targetOffer toff
        join fetch toff.user tu
        join fetch toff.book tb
        left join DeliveryOffer d on d.exchangeRequest.id = er.id
        where er.status = :status and d.id is null
        order by er.createdAt asc
        """)
    List<ExchangeRequest> findByStatusWithoutDeliveryOfferWithDetails(@Param("status") ExchangeRequestStatus status);
}
