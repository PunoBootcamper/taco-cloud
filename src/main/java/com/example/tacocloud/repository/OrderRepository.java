package com.example.tacocloud.repository;

import com.example.tacocloud.domain.TacoOrder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

public interface OrderRepository extends CrudRepository<TacoOrder,Long> {
    List<TacoOrder> findByDeliveryZip(String deliveryZip);
    List<TacoOrder> readOrdersByDeliveryZipAndPlacedAtBetween(
            String deliveryZip, Date startDate, Date endDate);
    List<TacoOrder> findByDeliveryNameAndDeliveryCityIgnoreCase(
            String deliveryName, String deliveryCity);
    List<TacoOrder> findByDeliveryCityOrderByDeliveryName(String city);
    @Query("SELECT o FROM TacoOrder o WHERE o.deliveryCity='Seattle'")
    List<TacoOrder> readOrdersDeliveredInSeattle();
}
