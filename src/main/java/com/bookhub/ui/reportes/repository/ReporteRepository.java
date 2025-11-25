package main.java.com.bookhub.ui.reportes.repository;

import com.bookhub.reportes.dto.ReporteSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReporteRepository extends JpaRepository<ReporteSistema, Long> {
}