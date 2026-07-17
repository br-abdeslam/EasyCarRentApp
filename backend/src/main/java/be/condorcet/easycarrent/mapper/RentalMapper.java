package be.condorcet.easycarrent.mapper;

import be.condorcet.easycarrent.dto.RentalRequestDto;
import be.condorcet.easycarrent.dto.RentalResponseDto;
import be.condorcet.easycarrent.entity.Customer;
import be.condorcet.easycarrent.entity.Rental;
import be.condorcet.easycarrent.entity.Vehicle;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link Rental} entities and their DTOs.
 *
 * <p>The referenced {@link Vehicle} and {@link Customer} and the backend
 * calculated total price are resolved by the service layer and passed in; this
 * mapper never queries repositories and makes no business decision.
 */
@Component
public class RentalMapper {

    /**
     * Builds a new rental from the request. The status defaults to
     * {@code PLANNED} through the entity constructor and {@code createdAt} is set
     * on persist.
     */
    public Rental toEntity(RentalRequestDto request, Vehicle vehicle, Customer customer, BigDecimal totalPrice) {
        return new Rental(
                vehicle,
                customer,
                request.startDate(),
                request.endDate(),
                totalPrice);
    }

    /**
     * Applies the editable request fields onto an existing rental. The id,
     * createdAt and status are intentionally left untouched so a normal update
     * never changes lifecycle state or the creation timestamp.
     */
    public void updateEntity(Rental rental, RentalRequestDto request, Vehicle vehicle,
                             Customer customer, BigDecimal totalPrice) {
        rental.setStartDate(request.startDate());
        rental.setEndDate(request.endDate());
        rental.setVehicle(vehicle);
        rental.setCustomer(customer);
        rental.setTotalPrice(totalPrice);
    }

    public RentalResponseDto toResponse(Rental rental) {
        Vehicle vehicle = rental.getVehicle();
        Customer customer = rental.getCustomer();
        return new RentalResponseDto(
                rental.getId(),
                rental.getStartDate(),
                rental.getEndDate(),
                rental.getStatus(),
                rental.getTotalPrice(),
                rental.getCreatedAt(),
                vehicle != null ? vehicle.getId() : null,
                vehicle != null ? vehicle.getRegistrationNumber() : null,
                vehicle != null ? vehicle.getBrand() : null,
                vehicle != null ? vehicle.getModel() : null,
                customer != null ? customer.getId() : null,
                customer != null ? customer.getFirstName() : null,
                customer != null ? customer.getLastName() : null);
    }
}
