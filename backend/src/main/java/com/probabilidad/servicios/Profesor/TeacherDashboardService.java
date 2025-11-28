// src/main/java/com/probabilidad/servicios/TeacherDashboardService.java
package com.probabilidad.servicios.Profesor;

import com.probabilidad.dto.StudentSummaryDto;
import com.probabilidad.dto.StudentQuizAttemptDto;
import com.probabilidad.dto.StudentAttemptsChartDto;
import com.probabilidad.dto.StudentPassedChartDto;
import com.probabilidad.entidades.dominio.EstadoIntento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class TeacherDashboardService {

    private static final Logger LOG = Logger.getLogger(TeacherDashboardService.class);

    @Inject
    EntityManager em;

    // ---------- YA EXISTENTES (resumen y lista de intentos) ----------

    public List<StudentSummaryDto> getStudentsWithAttempts() {
        LOG.debug("getStudentsWithAttempts() - Iniciando consulta de estudiantes");
        try {
            String jpql = """
                SELECT new com.probabilidad.dto.StudentSummaryDto(
                    a.id,
                    a.username,
                    a.email,
                    COUNT(i.id),
                    MAX(i.submittedAt),
                    AVG(i.score)
                )
                FROM IntentoQuiz i, Alumno a
                WHERE i.studentId = a.id
                GROUP BY a.id, a.username, a.email
                ORDER BY a.username ASC
                """;

            LOG.debugf("Ejecutando JPQL: %s", jpql);
            TypedQuery<StudentSummaryDto> q = em.createQuery(jpql, StudentSummaryDto.class);
            List<StudentSummaryDto> results = q.getResultList();
            LOG.debugf("getStudentsWithAttempts() - Retornando %d estudiantes", results.size());
            return results;
        } catch (Exception e) {
            LOG.error("Error en getStudentsWithAttempts()", e);
            throw e;
        }
    }

    public List<StudentQuizAttemptDto> getAttemptsForStudent(Long studentId) {
        String jpql = """
            SELECT new com.probabilidad.dto.StudentQuizAttemptDto(
                i.id,
                q.id,
                q.titulo,
                q.corte,
                i.startedAt,
                i.submittedAt,
                i.status,
                i.maxPoints,
                i.scorePoints,
                i.score
            )
            FROM IntentoQuiz i
              JOIN i.quiz q
            WHERE i.studentId = :studentId
            ORDER BY i.startedAt DESC
            """;

        TypedQuery<StudentQuizAttemptDto> q = em.createQuery(jpql, StudentQuizAttemptDto.class);
        q.setParameter("studentId", studentId);
        return q.getResultList();
    }

    // ---------- NUEVOS MÉTODOS PARA GRÁFICAS ----------

    /**
     * Top N estudiantes con más quices (intentos) realizados.
     */
    public List<StudentAttemptsChartDto> getTopStudentsByAttempts(int limit) {
        LOG.debugf("getTopStudentsByAttempts() - Iniciando con limit=%d", limit);
        try {
            String jpql = """
                SELECT new com.probabilidad.dto.StudentAttemptsChartDto(
                    a.id,
                    a.username,
                    COUNT(i.id)
                )
                FROM IntentoQuiz i, Alumno a
                WHERE i.studentId = a.id
                GROUP BY a.id, a.username
                ORDER BY COUNT(i.id) DESC, a.username ASC
                """;

            LOG.debugf("Ejecutando JPQL: %s", jpql);
            TypedQuery<StudentAttemptsChartDto> q =
                    em.createQuery(jpql, StudentAttemptsChartDto.class);
            q.setMaxResults(limit);
            List<StudentAttemptsChartDto> results = q.getResultList();
            LOG.debugf("getTopStudentsByAttempts() - Retornando %d resultados", results.size());
            return results;
        } catch (Exception e) {
            LOG.error("Error en getTopStudentsByAttempts()", e);
            throw e;
        }
    }

    /**
     * Top N estudiantes con más quices aprobados.
     * Un quiz se considera aprobado si tiene status PRESENTADO
     * y score >= minScore.
     */
    public List<StudentPassedChartDto> getTopStudentsByPassedQuizzes(BigDecimal minScore, int limit) {
        LOG.debugf("getTopStudentsByPassedQuizzes() - Iniciando con minScore=%s, limit=%d", minScore, limit);
        try {
            String jpql = """
                SELECT new com.probabilidad.dto.StudentPassedChartDto(
                    a.id,
                    a.username,
                    COUNT(i.id)
                )
                FROM IntentoQuiz i, Alumno a
                WHERE i.studentId = a.id
                  AND i.status = :status
                  AND i.score IS NOT NULL
                  AND i.score >= :minScore
                GROUP BY a.id, a.username
                ORDER BY COUNT(i.id) DESC, a.username ASC
                """;

            LOG.debugf("Ejecutando JPQL: %s", jpql);
            TypedQuery<StudentPassedChartDto> q =
                    em.createQuery(jpql, StudentPassedChartDto.class);
            q.setParameter("status", EstadoIntento.PRESENTADO);
            q.setParameter("minScore", minScore);
            q.setMaxResults(limit);
            List<StudentPassedChartDto> results = q.getResultList();
            LOG.debugf("getTopStudentsByPassedQuizzes() - Retornando %d resultados", results.size());
            return results;
        } catch (Exception e) {
            LOG.error("Error en getTopStudentsByPassedQuizzes()", e);
            throw e;
        }
    }
}
