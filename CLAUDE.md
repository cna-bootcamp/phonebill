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
$(cat claude/guide.md)
```

