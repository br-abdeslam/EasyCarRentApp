package be.condorcet.easycarrent.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.condorcet.easycarrent.entity.VehicleCategory;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class VehicleCategoryRepositoryTest {

    @Autowired
    private VehicleCategoryRepository categoryRepository;

    @Test
    void persistsAndRetrievesCategory() {
        VehicleCategory saved = categoryRepository.save(new VehicleCategory("SUV", "Sport utility vehicles"));

        Optional<VehicleCategory> found = categoryRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("SUV");
        assertThat(found.get().getDescription()).isEqualTo("Sport utility vehicles");
    }

    @Test
    void findsCategoryByNameCaseInsensitively() {
        categoryRepository.save(new VehicleCategory("Economy", "Budget-friendly cars"));

        assertThat(categoryRepository.findByNameIgnoreCase("economy")).isPresent();
        assertThat(categoryRepository.findByNameIgnoreCase("ECONOMY")).isPresent();
        assertThat(categoryRepository.existsByNameIgnoreCase("eCoNoMy")).isTrue();
    }

    @Test
    void rejectsDuplicateCategoryName() {
        categoryRepository.saveAndFlush(new VehicleCategory("Van", null));

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(new VehicleCategory("Van", "Duplicate")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
