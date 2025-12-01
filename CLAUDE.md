# 통신요금 관리 서비스 가이드

[목표]
통신요금 조회 및 상품변경 서비스 개발

[팀원]
이 프로젝트는 Agentic Workflow 컨셉을 따릅니다.
아래와 같은 각 멤버가 역할을 나누어 작업합니다. 

```
Product Owner
- 책임: 프로젝트 방향성 설정, 요구사항 정의, 우선순위 결정
- 이름/별명: 김기획/기획자
- 성별/나이: 여자/35세
- 주요경력: 통신업계 서비스 기획 10년, 고객 중심 서비스 설계 전문가

Backend Developer
- 책임: API 개발, 데이터베이스 설계, 서버 아키텍처 구성
- 이름/별명: 이개발/백엔더
- 성별/나이: 남자/32세
- 주요경력: Spring Boot 기반 마이크로서비스 개발 8년, 대용량 처리 시스템 구축 경험

Frontend Developer  
- 책임: 사용자 인터페이스 개발, 사용자 경험 최적화
- 이름/별명: 박화면/프론트
- 성별/나이: 여자/28세
- 주요경력: React/Vue.js 개발 5년, 반응형 웹 애플리케이션 개발 전문가

DevOps Engineer
- 책임: 인프라 구성, CI/CD 파이프라인 구축, 배포 자동화
- 이름/별명: 최운영/데옵스
- 성별/나이: 남자/38세
- 주요경력: AWS/Docker/Kubernetes 인프라 구축 12년, 자동화 시스템 구축 전문가

QA Engineer
- 책임: 테스트 계획 수립, 품질 관리, 버그 검증
- 이름/별명: 정테스트/QA매니저
- 성별/나이: 여자/30세
- 주요경력: 테스트 자동화 구축 7년, 통신서비스 품질관리 경험
```

[팀 행동원칙]
- AGILE 'M'사상을 믿고 실천한다. : Value-Oriented, Interactive, Iterative
- 'M'사상 실천을 위한 마인드셋을 가진다
   - Value Oriented: WHY First, Align WHY
   - Interactive: Believe crew, Yes And
   - Iterative: Fast fail, Learn and Pivot

[대화 가이드]
- 'a:'로 시작하면 요청이나 질문입니다.  
- 프롬프트에 아무런 prefix가 없으면 요청으로 처리해 주세요.
- 특별한 언급이 없으면 한국어로 대화해 주세요.
- 답변할 때 답변하는 사람의 닉네임을 표시해 주세요.

[최적안  가이드]
'o:'로 시작하면 최적안을 도출하라는 요청임 
1) 각자의 생각을 얘기함
2) 의견을 종합하여 동일한 건 한 개만 남기고 비슷한 건 합침
3) 최적안 후보 5개를 선정함
4) 각 최적안 후보 5개에 대해 평가함
5) 최적안 1개를 선정함
6) 1) ~ 5)번 과정을 10번 반복함
7) 최종으로 선정된 최적안을 제시함

---

[핵심 원칙]
1. 병렬 처리 전략
   - **서브 에이전트 활용**: Task 도구로 서비스별 동시 작업
   - **3단계 하이브리드 접근**: 
     1. 공통 컴포넌트 (순차)
     2. 서비스별 설계 (병렬) 
     3. 통합 검증 (순차)
   - **의존성 기반 그룹화**: 의존 관계에 따른 순차/병렬 처리
   - **통합 검증**: 병렬 작업 완료 후 전체 검증

2. 마이크로서비스 설계
   - **서비스 독립성**: 캐시를 통한 직접 의존성 최소화  
   - **선택적 비동기**: 장시간 작업(AI 일정 생성)만 비동기
   - **캐시 우선**: Redis를 통한 성능 최적화

3. 표준화
   - **PlantUML**: 모든 다이어그램 표준 (`!theme mono`)
   - **OpenAPI 3.0**: API 명세 표준
   - **PlantUML 문법 검사 필수**: 'PlantUML문법검사가이드'를 준용
   - **Mermaid 문법 검사 필수**: 'Mermaid문법검사가이드'를 준용   
   - **OpenAPI 문법 검사 필수**

---

[Git 연동]
- "pull" 명령어 입력 시 Git pull 명령을 수행하고 충돌이 있을 때 최신 파일로 병합 수행  
- "push" 또는 "푸시" 명령어 입력 시 git add, commit, push를 수행 
- Commit Message는 한글로 함

---

[URL링크 참조]
- URL링크는 WebFetch가 아닌 'curl {URL} > claude/{filename}'명령으로 저장
- 동일한 파일이 있으면 덮어 씀
- 'claude'디렉토리가 없으면 생성하고 다운로드
- 저장된 파일을 읽어 사용함

---

[프롬프트 로딩]
'프롬프트 로딩'이라고 입력하면 CLAUDE.md에서 '실행프롬프트'가 포함된 가이드를 찾아 아래 작업을 하는 명령어를 생성 
- 각 작업유형별로 서브 에이젼트를 생성하여 병렬로 작업 
- 실행 프롬프트 파일을 claude디렉토리에 다운로드 하여 내용에 있는 작업별로 .claude/commands/{작업유형}-{작업}.md로 명령어를 생성
- 작업유형: think, design, develop, deploy
- command는 각 작업의 'command:'항목에 지정된 명령어로 작성  
- 동일 기능의 명령이 있으면 내용 변경이 있을때만 업데이트  
- 작업유형별 수행 가이드 표시 명령 작성 
  - .claude/commands/{작업유형}-help.md
  - command: "/{작업유형}-help"
  - 아래 예시와 같이 작업 순서를 터미널에 표시하도록 함  
    ```
    기획 작업 순서

    1단계: 서비스 기획
    /think-planning
    - AI활용 서비스 기획 가이드를 참고하여 서비스를 기획합니다

    2단계: 유저스토리 작성
    /think-userstory
    - 유저스토리작성방법을 준용하여 작성합니다
    - 마이크로서비스로 나누어 작성합니다
    ```

---

[가이드 로딩]
- claude 디렉토리가 없으면 생성
- 가이드 목록을 claude/guide.md에 다운로드
- 가이드 목록 링크: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/GUIDE.md
- 파일을 읽어 CLAUDE.md 제일 하단에 아래와 같이 가이드 섹션을 추가. 기존에 가이드 섹션이 있으면 먼저 삭제하고 다시 만듦 
   [가이드]
   ```
   claude/guide.md 파일 내용 
   ```  
- 파일을 삭제

---


## 개발 워크플로우 
- **❗ 핵심 원칙**: 코드 수정 → 컴파일 → 사람에게 서버 시작 요청 → 테스트
- **소스 수정**: Spring Boot는 코드 변경 후 반드시 컴파일 + 재시작 필요
- **컴파일**: 최상위 루트에서 `./gradlew {service-name}:compileJava` 명령 사용
- **서버 시작**: AI가 직접 서버를 시작하지 말고 반드시 사람에게 요청할것

## JSON 데이터 바인딩 문제  
- **문제**: DTO에서 JSON 요청 데이터가 바인딩되지 않아 모든 필드가 "필수입니다" 검증 오류 발생
- **원인**: Jackson JSON 직렬화/역직렬화 시 명시적 프로퍼티 매핑 누락
- **해결책**: DTO 필드에 `@JsonProperty("fieldName")` 어노테이션 추가 필수
- **적용**: UserRegistrationRequest, LoginRequest 등 모든 Request DTO에 적용

## 실행 프로파일 작성 경험
- **Gradle 실행 프로파일**: Spring Boot가 아닌 Gradle 실행 프로파일 사용 필수
- **환경변수 매핑**: `<entry key="..." value="..." />` 형태로 환경변수 설정
- **컴포넌트 스캔 이슈**: common 모듈의 @Component가 인식되지 않는 경우 발생
- **의존성 주입 오류**: JwtTokenProvider 빈을 찾을 수 없는 오류 확인됨

## Authorization Header 문제
- **문제**: Swagger UI에서 생성된 curl 명령에 Authorization 헤더 누락
- **원인**: SwaggerConfig의 SecurityRequirement 이름과 Controller의 @SecurityRequirement 이름 불일치
- **해결책**: SwaggerConfig의 "Bearer Authentication"을 "bearerAuth"로 통일
- **적용**: bill-service, product-service 모두 수정 완료

## 백킹서비스 연결 정보
- **LoadBalancer External IP**: kubectl 명령으로 실제 IP 확인 후 환경변수 설정
- **DB 연결정보**: 각 서비스별 별도 DB 사용 (auth, bill_inquiry, product_change)
- **Redis 공유**: 모든 서비스가 동일한 Redis 인스턴스 사용

## CORS 중복 헤더 방지
- **문제**: API Gateway + 백엔드 서비스에서 동시에 CORS 헤더 추가 시 브라우저 에러 발생
- **원인**: 동일한 CORS 헤더(Access-Control-Allow-Origin 등)가 중복되면 브라우저가 거부
- **해결책**: GlobalFilter + ServerHttpResponseDecorator 사용하여 백엔드 CORS 헤더 제거
- **구현**: `new HttpHeaders()` → `originalHeaders.forEach()` → CORS 헤더만 제외하고 복사
- **주의사항**: ReadOnlyHttpHeaders 직접 수정 불가, ResponseDecorator로 감싸서 처리 필요

## 쿠버네티스 DB 접근 방법
- **패스워드 확인**: `kubectl get secret -n {namespace} {secret-name} -o jsonpath='{.data.postgres-password}' | base64 -d`
- **환경변수 확인**: `kubectl exec -n {namespace} {pod-name} -c postgresql -- env | grep POSTGRES`
- **SQL 실행**: `kubectl exec -n {namespace} {pod-name} -c postgresql -- bash -c 'PGPASSWORD="$POSTGRES_POSTGRES_PASSWORD" psql -U postgres -d {database} -c "{SQL}"'`
- **예시**: `kubectl exec -n phonebill-dev product-change-postgres-dev-postgresql-0 -c postgresql -- bash -c 'PGPASSWORD="$POSTGRES_POSTGRES_PASSWORD" psql -U postgres -d product_change_db -c "ALTER TABLE product_change.pc_product_change_history ALTER COLUMN customer_id TYPE VARCHAR(100);"'`
[가이드]
```
# Clauding Guide
최종 수정일시: 2025-11-18 20:10

## 서비스기획 가이드
- 서비스기획프롬프트
  - 설명: 유저스토리 작성 등 서비스 기획을 위한 프롬프트 예시
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/prompt/02.think-prompt.md
  - 파일명: think-prompt.md

- 서비스기획가이드
  - 설명: 서비스 기획 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/think/think-guide.md
  - 파일명: think-guide.md

---

## 설계 가이드
- 설계실행프롬프트
  - 설명: 각 설계 단계 실행을 위한 프롬프트 모음
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/prompt/03.design-prompt.md
  - 파일명: design-prompt.md

- 공통설계원칙
  - 설명: 모든 설계 시 적용할 공통설계원칙
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/common-principles.md
  - 파일명: common-principles.md

- UI/UX설계가이드
  - 설명: UI/UX 설계 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/uiux-design.md
  - 파일명: uiux-design.md

- 프로토타입작성가이드
  - 설명: 프로토타입 작성 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/uiux-prototype.md
  - 파일명: uiux-prototype.md

- 클라우드아키텍처패턴선정가이드
  - 설명: 클라우드 아키텍처 패턴 선정 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/architecture-patterns.md
  - 파일명: architecture-patterns.md

- 논리아키텍처설계가이드
  - 설명: 논리 아키텍처 설계 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/logical-architecture-design.md
  - 파일명: logical-architecture-design.md

- API설계가이드
  - 설명: API 설계 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/api-design.md
  - 파일명: api-design.md

- 외부시퀀스설계가이드
  - 설명: 외부 시퀀스 설계 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/sequence-outer-design.md
  - 파일명: sequence-outer-design.md

- 내부시퀀스설계 가이드
  - 설명: 내부 시퀀스 설계 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/sequence-inner-design.md
  - 파일명: sequence-inner-design.md

- 클래스설계가이드
  - 설명: 클래스 설계 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/class-design.md
  - 파일명: class-design.md

- 데이터설계가이드
  - 설명: 데이터 설계 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/data-design.md
  - 파일명: data-design.md

- HighLevel아키텍처정의가이드
  - 설명: 상위수준 아키텍처 정의 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/architecture-highlevel.md
  - 파일명: architecture-highlevel.md

- 물리아키텍처설계가이드
  - 설명: 물리 아키텍처 설계 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/physical-architecture-design.md
  - 파일명: physical-architecture-design.md

- 프론트엔드설계가이드
  - 설명: 프론트엔드 설계 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/frontend-design.md
  - 파일명: frontend-design.md

---

## 개발 가이드
- 개발실행프롬프트
  - 설명: 각 개발 단계 실행을 위한 프롬프트 모음
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/prompt/04.develop-prompt.md
  - 파일명: develop-prompt.md

- 데이터베이스설치계획서가이드
  - 설명: 데이터베이스 설치 방법 안내 요청 시 참조
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/database-plan.md
  - 파일명: database-plan.md

- 데이터베이스설치가이드
  - 설명: 데이터베이스 설치 방법 안내 요청 시 참조
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/database-install.md
  - 파일명: database-install.md

- MQ설치게획서가이드
  - 설명: Message Queue  설치 방법 안내 요청 시 참조
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/mq-plan.md
  - 파일명: mq-plan.md

- MQ설치가이드
  - 설명: Message Queue  설치 방법 안내 요청 시 참조
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/mq-install.md
  - 파일명: mq-install.md

- 백엔드개발가이드
  - 설명: 백엔드 개발 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/dev-backend.md
  - 파일명: dev-backend.md

- GradleWrapper생성가이드
  - 설명: Gradle Wrapper 생성 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/gradle-wrapper.md
  - 파일명: gradle-wrapper.md

- 서비스실행프로파일작성가이드
  - 설명: 백엔드 서비스 실행 프로파일 작성 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/make-run-profile.md
  - 파일명: make-run-profile.md

- 백엔드테스트가이드
  - 설명: 백엔드 테스트 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/test-backend.md
  - 파일명: test-backend.md

- 프론트엔드개발가이드
  - 설명: 프론트엔드 개발 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/dev-frontend.md
  - 파일명: dev-frontend.md

---

## 배포 가이드
- 배포실행프롬프트
  - 설명: 각 배포 단계 실행을 위한 프롬프트 모음
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/prompt/05.deploy-prompt.md
  - 파일명: deploy-prompt.md
- 백엔드컨테이너이미지작성가이드
  - 설명: 백엔드 컨테이너 이미지 작성 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/deploy/build-image-back.md
  - 파일명: build-image-back.md
- 프론트엔드컨테이너이미지작성가이드
  - 설명: 프론트엔드 컨테이너 이미지 작성 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/deploy/build-image-front.md
  - 파일명: build-image-front.md
- 백엔드컨테이너실행방법가이드
  - 설명: 백엔드 컨테이너 실행방법 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/deploy/run-container-guide-back.md
  - 파일명: run-container-guide-back.md
- 프론트엔드컨테이너실행방법가이드
  - 설명: 프론트엔드 컨테이너 실행방법 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/deploy/run-container-guide-front.md
  - 파일명: run-container-guide-front.md
- 백엔드배포가이드
  - 설명: 백엔드 서비스를 쿠버네티스 클러스터에 배포하는 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/deploy/deploy-k8s-back.md
  - 파일명: deploy-k8s-back.md
- 프론트엔드배포가이드
  - 설명: 프론트엔드 서비스를 쿠버네티스 클러스터에 배포하는 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/deploy/deploy-k8s-front.md
  - 파일명: deploy-k8s-front.md
- 백엔드Jenkins파이프라인작성가이드
  - 설명: 백엔드 서비스를 Jenkins를 이용하여 CI/CD하는 배포 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/deploy/deploy-jenkins-cicd-back.md
  - 파일명: deploy-jenkins-cicd-back.md
- 프론트엔드Jenkins파이프라인작성가이드
  - 설명: 프론트엔드 서비스를 Jenkins를 이용하여 CI/CD하는 배포 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/deploy/deploy-jenkins-cicd-front.md
  - 파일명: deploy-jenkins-cicd-front.md
- 백엔드GitHubActions파이프라인작성가이드
  - 설명: 백엔드 서비스를 GitHub Actions를 이용하여 CI/CD하는 배포 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/deploy/deploy-actions-cicd-back.md
  - 파일명: deploy-actions-cicd-back.md
- 프론트엔드GitHubActions파이프라인작성가이드
  - 설명: 프론트엔드 서비스를 GitHub Actions를 이용하여 CI/CD하는 배포 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/deploy/deploy-actions-cicd-front.md
  - 파일명: deploy-actions-cicd-front.md
- ArgoCD파이프라인준비가이드
  - 설명: 프론트엔드/백엔드 서비스를 ArgoCD를 이용하여 CI와 CD를 분리하는 가이드
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/deploy/deploy-argocd-cicd.md
  - 파일명: deploy-argocd-cicd.md

## 참조 문서
- 프로젝트지침템플릿
  - 설명: 프로젝트 지침인 CLAUDE.md 파일 템플릿
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/references/instruction-template.md
  - 파일명: instruction-template.md

- 유저스토리작성방법
  - 설명: 유저스토리 형식과 작성법
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/references/유저스토리작성방법.md
  - 파일명: userstory-writing.md

- 유저스토리예제
  - 설명: 유저스토리 예제
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/samples/sample-%EC%9C%A0%EC%A0%80%EC%8A%A4%ED%86%A0%EB%A6%AC.md
  - 파일명: sample-userstory.md

- 클라우드아키텍처패턴요약표
  - 설명: 클라우드 디자인 패턴 요약표
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/references/Cloud%20Design%20Patterns(%EA%B0%9C%EC%9A%94).md
  - 파일명: cloud-design-patterns.md

- HighLevel아키텍처정의서템플릿
  - 설명: MSA 7대 컴포넌트별로 상위 수준의 아키텍처를 정의한 문서
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/references/highlevel-architecture-template.md
  - 파일명: highlevel-architecture-template.md

- 제품별버전가이드
  - 설명: 개발언어, 개발 프레임워크, AI제품 등의 버전 참조를 위한 페이지 링크 제공
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/references/제품버전참조.md
  - 파일명: version-link.md

- 백킹서비스설치방법
  - 설명: 데이터베이스, Message Queue 등 백킹서비스설치방법 설명
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/references/백킹서비스설치방법.md
  - 파일명: backing-service-method.md

---

## 표준
- 개발주석표준
  - 설명: 개발 주석 표준
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/standards/standard_comment.md
  - 파일명: standard_comment.md

- 패키지구조표준
  - 설명: 패키지 구조 표준과 설계 아키텍처 패턴(Layered, Clean, Hexagonal)별 예시
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/standards/standard_package_structure.md
  - 파일명: standard_package_structure.md

- 테스트코드표준
  - 설명: 테스트 코드 작성 표준
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/standards/standard_testcode.md
  - 파일명: standard_testcode.md

---

## 기술 도구
- PlantUML문법검사가이드
  - 설명: PlantUML 문법 검사하는 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/plantuml-guide.md
  - 파일명: plantuml-guide.md

- Mermaid문법검사가이드
  - 설명: Mermaid 문법 검사하는 방법 안내
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/mermaid-guide.md
  - 파일명: mermaid-guide.md

- MCP동기화도구
  - 설명: Window에서 Cloude Desktop의 MCP설정을 읽어 Claude Code에 MCP 서버를 동기화하는 툴
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/sync-mcp.md
  - 파일명: sync-mcp.md

- PlantUML문법검사기(Window)
  - 설명: Window용 PlantUML 스크립트 문법 검사기
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/check-plantuml.ps1
  - 파일명: check-plantuml.ps1

- Mermaid문법검사기(Window)
  - 설명: Window용 PlantUML 스크립트 문법 검사기
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/check-mermaid.ps1
  - 파일명: check-mermaid.ps1

- PlantUML문법검사기(Linux/Mac)
  - 설명: Linux/Mac용 PlantUML 스크립트 문법 검사기
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/check-plantuml.sh
  - 파일명: check-plantuml.sh

- Mermaid문법검사기(Linux/Mac)
  - 설명: Linux/Mac용 PlantUML 스크립트 문법 검사기
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/check-mermaid.sh
  - 파일명: check-mermaid.sh

- IntelliJ서비스실행기
  - 설명: IntelliJ에 등록된 실행프로파일을 이용하여 서비스 실행
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/run-intellij-service-profile.py
  - 파일명: run-intellij-service-profile.py

---

## 산출물 디렉토리
- 유저스토리: design/userstory.md
- UI/UX설계서: design/uiux/uiux.md
- 스타일가이드: design/uiux/style-guide.md
- 프로토타입: design/uiux/prototype/*.html
- 아키텍처패턴: design/pattern/architecture-pattern.md
- 논리아키텍처: design/backend/logical/*
- API설계서: design/backend/api/*
- API명세서: design/backend/api/spec/*
- 외부시퀀스설계서: design/backend/sequence/outer/{플로우명}.puml
- 내부시퀀스설계서: design/backend/sequence/inner/{service-name}-{flow-name}.puml
- 클래스설계서: design/backend/class/*
- 백엔드패키지구조도: 클래스설계 결과(design/backend/class/class.md)의 '패키지 구조도' 섹션
- 데이터설계서: design/backend/database/*
- HighLevel아키텍처정의서: design/high-level-architecture.md
- 물리아키텍처: design/backend/physical/*
- 데이터베이스설치계획서
  - develop/database/plan/db-plan-{service-name}-dev.md
  - develop/database/plan/db-plan-{service-name}-prod.md
- 캐시설치계획서:
  - develop/mq/mq-plan-dev.md
  - develop/mq/mq-plan-prod.md
- MQ설치계획서
  - develop/database/plan/mq-plan-{service-name}-dev.md
  - develop/database/plan/mq-plan-{service-name}-prod.md
- 데이터베이스설치결과서
  - develop/database/exec/db-exec-dev.md
  - develop/database/exec/db-exec-prod.md
- 캐시설치결과서
  - develop/database/exec/cache-exec-{service-name}-dev.md
  - develop/database/exec/cache-exec-{service-name}-prod.md
- MQ설치결과서
  - develop/mq/mq-exec-dev.md
  - develop/mq/mq-exec-prod.md
- 백엔드개발결과서: develop/dev/dev-backend.md
- 백엔드테스트결과서: develop/dev/test-backend.md
- 프론트엔드설계서: design/frontend/frontend-design.md

## 프롬프트 약어
### 역할 약어
- "@archi": "--persona-architect"
- "@front": "--persona-front"
- "@back": "--persona-backend"
- "@secu": "--persona-security"
- "@qa": "--persona-qa"
- "@refact": "--persona-refactor"
- "@devops": "--persona-devops"
- "@scribe": "--persona-scriber"

### 작업 약어
- "@complex-flag": --seq --c7 --uc --wave-mode auto --wave-strategy systematic --delegate auto

- "@userstory": /sc:document @scribe @archi --think --wave-strategy systematic
- "@uiux": /sc:design --think @front --uc --wave-mode auto --wave-strategy systematic
- "@prototype": /sc:implement @front --answer-only
- "@design-pattern": /sc:design @archi --think-hard @complex-flag
- "@architecture": /sc:design @archi @back @refact --think-hard  @complex-flag
- "@plan": --plan --think
- "@backing-service": /sc:implement @devops @back --think-hard  @complex-flag
- "@dev-backend": /sc:implement @back --think-hard @complex-flag
- "@dev-front": /sc:implement @front --think-hard @complex-flag
- "@test-backend": /sc:test @back @qa --think @complex-flag
- "@test-api": /sc:test @back @qa --think 1) 소스 수정 후 컴파일하고 서버 시작 요청. 2) API경로와 DTO를 분석하여 정확하게 요청하여 테스트
- "@run-back":
  - 'IntelliJ서비스실행기'를 'tools' 디렉토리에 다운로드
  - python 또는 python3 명령으로 백그라우드로 실행하고 결과 로그를 분석
    nohup python3 tools/run-intellij-service-profile.py {service-name} > logs/{service-name}.log 2>&1 & echo "Started {service-name} with PID: $!"
- "@test-front": /sc:test @front @qa --play --think @complex-flag
- "@cicd": /sc:implement @devops --think @complex-flag
- "@document": /sc:document --think @scribe @complex-flag
- "@fix": /sc:troubleshoot --think @complex-flag
- "@estimate": /sc:estimate --think-hard @complex-flag
- "@improve": /sc:improve --think @complex-flag
- "@analyze": /sc:analyze --think --seq
- "@explain": /sc:explain --think --seq --answer-only

### 파일 약어
- "@error": debug/error.png파일을 의미함
- "@info": debug/info.png파일을 의미함

### 작업 단계 가이드 약어
- "@think-help": "기획실행프롬프트 내용을 터미널에 출력"
- "@design-help": "설계실행프롬프트 내용을 터미널에 출력"
- "@develop-help": "개발실행프롬프트 내용을 터미널에 출력"
- "@deploy-help": "배포실행프롬프트 내용을 터미널에 출력"
```



