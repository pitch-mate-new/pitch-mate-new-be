package com.example.pitchmateserver.rubric;

import com.example.pitchmateserver.rubric.entity.Rubric;
import com.example.pitchmateserver.rubric.repository.RubricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RubricRepository rubricRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (rubricRepository.count() > 0) return;

        rubricRepository.saveAll(List.of(
                Rubric.builder().title("내용 구성").description("발표 내용이 논리적으로 잘 구성되어 있는가").maxScore(5).displayOrder(1).build(),
                Rubric.builder().title("전달력").description("청중이 이해하기 쉽게 내용을 전달하는가").maxScore(5).displayOrder(2).build(),
                Rubric.builder().title("말하기 속도").description("적절한 속도로 말하는가").maxScore(5).displayOrder(3).build(),
                Rubric.builder().title("발음 및 억양").description("발음이 정확하고 억양이 자연스러운가").maxScore(5).displayOrder(4).build(),
                Rubric.builder().title("시선 처리").description("청중과 적절한 눈 맞춤을 유지하는가").maxScore(5).displayOrder(5).build(),
                Rubric.builder().title("자세 및 제스처").description("자세와 제스처가 발표에 적합한가").maxScore(5).displayOrder(6).build(),
                Rubric.builder().title("시간 관리").description("주어진 시간 내에 발표를 완료하는가").maxScore(5).displayOrder(7).build()
        ));
    }
}
