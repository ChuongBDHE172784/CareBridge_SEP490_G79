---
title: "WHO Digital Adaptation Kit cho chăm sóc trước sinh: yêu cầu vận hành để số hóa khuyến nghị"
stage: PREGNANCY
topic: HEALTH_MONITORING
document_type: GUIDELINE
source: "Digital adaptation kit for antenatal care: operational requirements for implementing WHO recommendations in digital systems, 2021"
organization: "World Health Organization (WHO)"
section: "SMART Guidelines, luồng nghiệp vụ, dữ liệu lõi, logic hỗ trợ quyết định và yêu cầu hệ thống ANC"
---

# WHO Digital Adaptation Kit cho chăm sóc trước sinh: yêu cầu vận hành để số hóa khuyến nghị

## Mục tiêu của Digital Adaptation Kit (DAK)

### Cầu nối giữa chuyên môn y tế và phát triển phần mềm
DAK tạo “ngôn ngữ chung” cho quản lý chương trình sức khỏe bà mẹ, chuyên gia y tế số, business analyst và nhà phát triển. Mục tiêu là bảo đảm nội dung số tuân thủ khuyến nghị WHO, cho phép kiểm tra minh bạch tính đúng đắn của nội dung lâm sàng và cung cấp điểm xuất phát cho **DTDS (Digital Tracking and Decision-Support - theo dõi số và hỗ trợ quyết định)** trong chăm sóc trước sinh.

### Không dùng nguyên xi mà phải bản địa hóa
Các workflow, dữ liệu và thuật toán trong DAK là mẫu chung được dẫn xuất từ khuyến nghị WHO 2016 về ANC và các tài liệu liên quan. WHO nhấn mạnh chúng **phải được contextualize (bản địa hóa)** theo chính sách, phạm vi hành nghề, nguồn lực và quy trình quốc gia/địa phương. Khi hệ thống hiện có khác DAK, cần ghi nhận và giải thích rõ lý do sai khác thay vì âm thầm sửa nội dung.

## Tám thành phần liên kết của DAK

### 1–4: Từ khuyến nghị đến quy trình
1. **Health interventions and recommendations:** danh mục can thiệp và khuyến nghị WHO cần số hóa.  
2. **Generic personas:** vai trò người dùng mục tiêu, chủ yếu nhân viên y tế có năng lực ANC tại tuyến chăm sóc ban đầu; cần bản địa hóa theo nghề nghiệp, kỹ năng số, kết nối mạng, khoảng cách chuyển tuyến.  
3. **User scenarios:** câu chuyện mô tả cách các persona tương tác với nhau và hệ thống.  
4. **Business processes and workflows:** mô hình hóa quy trình nghiệp vụ, hoạt động, điểm quyết định và luồng thông tin.

### 5–8: Từ quy trình đến yêu cầu phần mềm
5. **Core data elements:** từ điển dữ liệu lõi, định nghĩa dữ liệu cần thu thập/trao đổi.  
6. **Decision-support logic:** bảng/thuật toán logic quyết định dùng để tự động hóa hoặc hỗ trợ nhân viên y tế.  
7. **Indicators and performance metrics:** chỉ số và yêu cầu báo cáo để giám sát triển khai/chất lượng.  
8. **Functional and non-functional requirements:** yêu cầu chức năng và phi chức năng cho hệ thống.

## Quy ước định danh hữu ích cho CareBridge

### ID có thể dùng để truy vết giữa guideline và code
DAK gợi ý định danh có cấu trúc: quy trình như **ANC.A, ANC.B**; hoạt động theo Process ID + số hoạt động; data element dạng **ANC.[process].DE.[n]**; decision table dạng **ANC.DT.[n]**; indicator dạng **ANC.IND.[n]**; functional requirement dạng **ANC.REQ.[n]** và non-functional requirement dạng **ANC.NFXNREQ.[n]**. Với CareBridge, nguyên tắc này rất phù hợp để gắn mỗi chunk RAG/clinical rule với nguồn khuyến nghị và logic hệ thống.

## Cách áp dụng cho RAG/clinical decision support

### Trường hợp hệ thống đã có sẵn
Dùng DAK để đối chiếu workflow, trường dữ liệu và logic hỗ trợ quyết định trong EMR/CDSS hiện tại với khuyến nghị WHO; xác định các trường thiếu, logic sai khác và lý do local adaptation. Không chỉ đối chiếu văn bản, mà phải kiểm tra mối liên hệ giữa **dữ liệu đầu vào → điều kiện logic → khuyến nghị/nhắc việc → chỉ số báo cáo**.

### Trường hợp chuyển từ giấy sang số
DAK có thể làm bộ khung tối thiểu để chọn dữ liệu thật sự cần thiết, chuẩn hóa form điện tử và xây dựng decision support. Kiến trúc nên mô-đun hóa để liên kết ANC với kế hoạch hóa gia đình, hậu sản, tiêm chủng, HIV/STI và các dịch vụ chăm sóc ban đầu khác.

## Nội dung nguồn đã làm sạch để bảo toàn tri thức

> Phần dưới giữ nguyên thuật ngữ và câu chữ tiếng Anh của tài liệu gốc sau khi loại bỏ tạp âm hành chính, header/footer, phần tham khảo và một số đoạn phương pháp thống kê thuần túy. Lớp tiếng Việt phía trên là lớp chuẩn hóa semantic cho truy xuất; khi cần kiểm chứng số liệu/thuật ngữ, ưu tiên đối chiếu phần nguồn này.

### Nguồn gốc: recommendations — đoạn 001

personas

### Nguồn gốc: Adaptation Kit — đoạn 002

for Antenatal

### Nguồn gốc: Care — đoạn 003

scenarios workflows

### Nguồn gốc: Operational requirements — đoạn 004

for implementing WHO

### Nguồn gốc: recommendations in — đoạn 005

digital systems

### Nguồn gốc: recommendations in — đoạn 006

data decisions indicators

### Nguồn gốc: SMART GUIDELINES — đoạn 007

A

### Nguồn gốc: SMART GUIDELINES — đoạn 008

requirements

### Nguồn gốc: Adaptation Kit — đoạn 009

for Antenatal

### Nguồn gốc: Operational requirements — đoạn 010

for implementing WHO

### Nguồn gốc: recommendations in — đoạn 011

digital systems

### Nguồn gốc: ANC — đoạn 012

antenatal care

### Nguồn gốc: ANM — đoạn 013

auxiliary nurse midwife

### Nguồn gốc: API — đoạn 014

application programming interface

### Nguồn gốc: ASB — đoạn 015

asymptomatic bacteriuria

### Nguồn gốc: BMI — đoạn 016

body mass index

### Nguồn gốc: BPMN — đoạn 017

business process model and notation

### Nguồn gốc: CDS — đoạn 018

clinical decision support

### Nguồn gốc: CQL — đoạn 019

clinical quality language

### Nguồn gốc: CHW — đoạn 020

community health worker

### Nguồn gốc: DAK — đoạn 021

digital adaptation kit

### Nguồn gốc: DBP — đoạn 022

diastolic blood pressure

### Nguồn gốc: DBP — đoạn 023

DE

### Nguồn gốc: DBP — đoạn 024

data element

### Nguồn gốc: Digital investment implementation guide — đoạn 025

DM

### Nguồn gốc: Digital investment implementation guide — đoạn 026

diabetes mellitus

### Nguồn gốc: Decision Model Notation — đoạn 027

DT

### Nguồn gốc: Decision Model Notation — đoạn 028

decision-support table

### Nguồn gốc: DTDS — đoạn 029

digital tracking and decision support

### Nguồn gốc: DTDS — đoạn 030

eHealth

### Nguồn gốc: DTDS — đoạn 031

electronic health

### Nguồn gốc: EDD — đoạn 032

estimated due date

### Nguồn gốc: EMR — đoạn 033

electronic medical record

### Nguồn gốc: Fast Health Interoperability Resources — đoạn 034

GA

### Nguồn gốc: Fast Health Interoperability Resources — đoạn 035

gestational age

### Nguồn gốc: GDM — đoạn 036

gestational diabetes mellitus

### Nguồn gốc: HEADSS — đoạn 037

home, education, activities/employment, drugs, suicidality and sex

### Nguồn gốc: HL7 — đoạn 038

Health level 7, Inc.

### Nguồn gốc: HIS — đoạn 039

health information system

### Nguồn gốc: HMIS — đoạn 040

health management information system

### Nguồn gốc: ICT — đoạn 041

information and communications technology

### Nguồn gốc: ICT — đoạn 042

ID

### Nguồn gốc: ICT — đoạn 043

identification

### Nguồn gốc: IFA — đoạn 044

iron and folic acid

### Nguồn gốc: IPTp — đoạn 045

intermittent preventive treatment in pregnancy

### Nguồn gốc: IPV — đoạn 046

intimate partner violence

### Nguồn gốc: LMP — đoạn 047

last menstrual period

### Nguồn gốc: M&E — đoạn 048

monitoring and evaluation

### Nguồn gốc: MAPS — đoạn 049

mHealth Assessment and Planning for Scale

### Nguồn gốc: NFXNREQ — đoạn 050

non-functional requirement

### Nguồn gốc: NFXNREQ — đoạn 051

mHealth

### Nguồn gốc: MOH — đoạn 052

ministry of health v

### Nguồn gốc: PICT — đoạn 053

provider-initiated counselling and testing

### Nguồn gốc: PNC — đoạn 054

postnatal care

### Nguồn gốc: PrEP — đoạn 055

pre-exposure prophylaxis

### Nguồn gốc: PrEP — đoạn 056

QR

### Nguồn gốc: Sexual and Reproductive Health and Research (WHO — đoạn 057

department)

### Nguồn gốc: SRHR — đoạn 058

sexual and reproductive health and rights

### Nguồn gốc: SBP — đoạn 059

systolic blood pressure

### Nguồn gốc: SFH — đoạn 060

symphysis fundal height

### Nguồn gốc: STI — đoạn 061

sexually transmitted infection

### Nguồn gốc: SMS — đoạn 062

short message service (text message)

### Nguồn gốc: SOP — đoạn 063

standard operating procedure

### Nguồn gốc: SOP — đoạn 064

SP

### Nguồn gốc: SOP — đoạn 065

sulfadoxine–pyrimethamine

### Nguồn gốc: SOP — đoạn 066

TB

### Nguồn gốc: SOP — đoạn 067

tuberculosis

### Nguồn gốc: TTCV — đoạn 068

tetanus toxoid-containing vaccine

### Nguồn gốc: UHC — đoạn 069

universal health coverage

### Nguồn gốc: World Health Organization — đoạn 070

vi

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 071

overview

### Nguồn gốc: Overview — đoạn 072

overview

### Nguồn gốc: Background — đoạn 073

Digital health – defined broadly as the systematic application of information and communications technologies, computer science and data to support informed decision-making by individuals, the health workforce and health systems, to strengthen resilience to disease and improve health and wellness (1) – is increasingly being applied as an essential enabler for health service delivery and accountability. Ministries of health have recognized the value of digital health as articulated within the World Health Assembly resolution (2) and the Global strategy on digital health (3). Likewise, donors have advocated for the rational use of digital tools as part of efforts to expand coverage and quality of services, as well as promote data use and monitoring efforts (4–6).

### Nguồn gốc: Background — đoạn 074

Despite the investments into and abundance of digital systems, there is often limited understanding of and transparency in the health data and logic contained in these digital tools, or relationship with evidence-based clinical or public health recommendations, which not only undermines the credibility of such systems, but also impedes opportunities for interoperability and threatens potential for continuity of care.

### Nguồn gốc: Background — đoạn 075

Evidence-based recommendations, such as those featured in WHO guidelines, establish standards of care and offer a reference point for informing the content of digital systems that countries adopt. However, guidelines are often only available in a narrative format that requires a resource-intensive process to be elaborated into the specifications needed for digital systems. This translation of guidelines for digital systems often results in subjective interpretation for implementers and software vendors, which can lead to inconsistencies or inability to verify the content within these systems, potentially leading to adverse health outcomes and other unintended effects.

### Nguồn gốc: Background — đoạn 076

Additionally, where digital systems exist, the documentation of the underlying data and content may be unavailable or proprietary, requiring governments to start from scratch and expend additional resources each time they intend to deploy such a system. Furthermore, this lack of documentation of the health content can lead to dependence on one vendor and haphazard deployments that are unscalable or difficult to replicate across different settings.

### Nguồn gốc: Background — đoạn 077

To ensure countries can effectively benefit from digital health investments, “digital adaptation kits” (DAKs) are designed to facilitate the accurate reflection of WHO’s clinical, public health and data use guidelines within the digital systems countries are adopting. DAKs are operational, softwareneutral, standardized documentation that distil clinical, public health and data use guidance into a format that can be transparently incorporated into digital systems. Although digital implementations comprise multiple factors – including (i) the health domain data and content; (ii) the digital intervention or functionality; and (iii) the digital application or communication channel for delivering the digital intervention – DAKs focus primarily on ensuring the validity of the health content (see Figure 1) (7). Accordingly, DAKs provide the generic content requirements that should be housed within digital systems, independently of a specific software application and with the intention that countries can customize them to local needs.

### Nguồn gốc: The DAKs follow a modular approach in detailing the — đoạn 078

data and content requirements for a specific health programme area – such as antenatal care, family planning, sexually transmitted infections (STIs) – among the different health areas for which DAKs have been developed. This specific DAK focuses on providing the content requirements for a digital tracking and decisionsupport system used by health workers during antenatal care (ANC) and is based on the WHO ANC guideline:

### Nguồn gốc: WHO recommendations on antenatal care for a positive — đoạn 079

pregnancy experience (8). It also includes cross-cutting elements focused on the client, such as self-care interventions, though these interventions are described from the perspective of the health worker, not from that of the clients.

### Nguồn gốc: Overview — đoạn 080

overview

### Nguồn gốc: For this particular DAK, the requirements are based — đoạn 081

on systems that provide the functionalities of digital tracking and decision support (see Box 1) and include components such as personas, workflows, core data elements, decision-support algorithms, scheduling logic and reporting indicators. Operational outputs, such as spreadsheets of the data dictionary and the detailed decision-support algorithms, are included as part of the DAK as practical resources that implementers can use as starting points when developing digital systems.

### Nguồn gốc: Furthermore, data components within the DAK are — đoạn 082

mapped to standards-based terminology, such as the

### Nguồn gốc: International Classification of Diseases (ICD), to facilitate — đoạn 083

interoperability.

### Nguồn gốc: International Classification of Diseases (ICD), to facilitate — đoạn 084

FIG. 1

### Nguồn gốc: Digital adaptation kits and their role in digital — đoạn 085

health implementations

### Nguồn gốc: Information that is aligned with — đoạn 086

recommended health practices or validated health content

### Nguồn gốc: Information that is aligned with — đoạn 087

+

### Nguồn gốc: A discrete function of digital — đoạn 088

technology to achieve healthsector objectives

### Nguồn gốc: A discrete function of digital — đoạn 089

+

### Nguồn gốc: ICT systems and communication — đoạn 090

channels that facilitate delivery of the digital interventions and health content

### Nguồn gốc: INFRASTRUCTURE — đoạn 091

overview

### Nguồn gốc: INFRASTRUCTURE — đoạn 092

BOX 1

### Nguồn gốc: What is digital tracking and decision support? — đoạn 093

Digital tracking is the use of digitized records to capture and store clients’ health information to enable follow-up of their health status and services received. This may include digital forms of paper-based registers and case management logs within specific target populations, as well as electronic patient records linked to uniquely identified individuals (7,9).

### Nguồn gốc: What is digital tracking and decision support? — đoạn 094

Digital tracking makes it possible to register and follow up patient services, and may be done through an electronic medical record (EMR) or other digital forms of health records. Digital tracking aims to reduce lapses in continuity of care by stimulating timely follow-up contacts, and may incorporate decisionDigital health

### Nguồn gốc: Expected — đoạn 095

intervention support tools to guide health workers in: executing clinical protocols to deliver appropriate care, scheduling upcoming services and following checklists

### Nguồn gốc: Contribution — đoạn 096

(accessible at a

### Nguồn gốc: Recommendation — đoạn 097

to universal health for appropriate case management at point of care. Some other descriptors include “digital minimum viaversions mobile of paper-based registers for specific health domains; coverage (UHC) devices) digitized registers for longitudinal health programmes including tracking of migrant populations’ benefits and health status; case management logs within specific target populations, including migrant populations” (9).

### Nguồn gốc: Recommendation 6: WHO recommends digital targeted client — đoạn 098

communication communication for health issues regarding sexual, reproductive,

### Nguồn gốc: Health worker decision support is defined as: “digitized jobContinuous — đoạn 099

aids that combine an individual’s health information with the health worker’s knowledge and maternal, newborn, and child health under the condition that coverage clinical protocols to assist health workers in making diagnosis and treatment decisions” (9). Thus, a person-centred tracking and and decision-support potential concernsdigital about sensitive content data privacy can be (DTDS) system is one used by health workers at the point of care; it includes a persistent record of health events and encounters that links to clinical addressed decision-support systems to reinforce good practice. It also links to reporting and management tools(Recommended to reinforceonly accountability.

### Nguồn gốc: A DTDS — đoạn 100

record includes all in specific contexts or conditions) the information required for detailing an individual’s health status and the health interventions provided to them.

### Nguồn gốc: Recommendation 7: WHO recommends the use of digital decision — đoạn 101

decision support support accessible devices for community and facilityDTDS end-users are all cadres of health-care providers operating at all care levels, including those operating outsideviaofmobile formal health-care facilities (e.g. based health workers in the context of tasks that are already defined community health workers, health volunteers). DTDS systems emphasize the use of “collect once, use for many purposes” (10), in which data collected for within the scope of practice for the health worker.

### Nguồn gốc: Recommendation 7: WHO recommends the use of digital decision — đoạn 102

service delivery can also be used for accountability (i.e. they can be used to calculate aggregate indicators required for reporting, including monitoring (Recommended only in specific contexts or conditions) provider, stock and system performance).

### Nguồn gốc: WHO has provided the following — đoạn 103

context-specific recommendation for the use of an integrated system that provides both a digital track of client’s health status and decision support (7).

### Nguồn gốc: Accountability — đoạn 104

coverage

### Nguồn gốc: Digital tracking of — đoạn 105

clients’ health status and services (digital tracking) combined with decision support

### Nguồn gốc: Recommendation 8: WHO recommends digital tracking of clients’ — đoạn 106

health status and services, combined with decision support under these conditions:

### Nguồn gốc: Ⱥ in settings where the health system can support the — đoạn 107

implementation of these intervention components in an integrated manner; and Ⱥ for tasks that are already defined as within the scope of practice for the health worker. (Recommended only in specific contexts or conditions)

### Nguồn gốc: Accountability — đoạn 108

coverage

### Nguồn gốc: Recommendation 9: WHO recommends the use of digital tracking — đoạn 109

combined with: combined with decision support and targeted client communication (a) decision under these conditions: supportDigital and

### Nguồn gốc: Kithealth — đoạn 110

for system

### Nguồn gốc: Ⱥ where the — đoạn 111

can support (b) targeted client

### Nguồn gốc: Ⱥ where the — đoạn 112

intervention components in an integrated manner;

### Nguồn gốc: Ⱥ where the — đoạn 113

The operational and standardized documentation reflected within the DAKs represent one of the steps within a broader vision of Standardsbased, Machine-readable, Adaptive, Requirements-based, and Testable (SMART) Guidelines. SMART Guidelines aim to maximize health impact through improved fidelity and uptake of recommendations through a systematic process for transforming guideline development, delivery and application (11,12). Within this vision, DAKs serve as a prerequisite for developing computable, or machine-readable, guidelines, as well as executable reference software and advanced analytics for precsion health. Figure 2 provides an overview of the different layers of the SMART Guidelines continuum and where DAKs fit within this strategy (11).

### Nguồn gốc: Ⱥ where the — đoạn 114

FIG. 2

### Nguồn gốc: Paper — đoạn 115

systems

### Nguồn gốc: Narrative — đoạn 116

guidelines

### Nguồn gốc: Evidence-based guideline recommendations and accompanying — đoạn 117

implementation and data guidance

### Nguồn gốc: Digital — đoạn 118

adaptation kits

### Nguồn gốc: Digital — đoạn 119

“Human readable” software-neutral documentation of operational and functional requirements (e.g. personas, workflows, relevant metadata, transparently documented algorithms, minimum data sets, priority metrics, listing of relevant health interventions, functional requirements)

### Nguồn gốc: Structured software-neutral specifications, code, terminology and — đoạn 120

readable interoperability standards

### Nguồn gốc: Reference — đoạn 121

software

### Nguồn gốc: Software that are able to execute executable static algorithms and — đoạn 122

interoperable digital components to deliver the operational and functional requirements

### Nguồn gốc: Precision — đoạn 123

health model

### Nguồn gốc: Executable dynamic algorithms that are trained and optimized with — đoạn 124

advanced analytics to achieve prioritized outcomes

### Nguồn gốc: Smart L5 Dynamic — đoạn 125

digital systems

### Nguồn gốc: Overview — đoạn 126

overview

### Nguồn gốc: Digital adaptation kits within a strategic vision for SMART Guidelines — đoạn 127

overview

### Nguồn gốc: Objectives — đoạn 128

This DAK focuses on antenatal care (ANC) and aims to provide a common language across various audiences – maternal health and other programme managers, software developers, and implementers of digital systems – to ensure a common understanding of the appropriate health information content within the ANC health programme area, as a mechanism to catalyse the effective use of these digital systems. The key objectives of this DAK are: »

### Nguồn gốc: Objectives — đoạn 129

to ensure adherence to WHO clinical, public health and data use guidelines, and facilitate consistency of the health content that is used to inform the development of a person-centred digital tracking and decision-support (DTDS) system;

### Nguồn gốc: Objectives — đoạn 130

»

### Nguồn gốc: Objectives — đoạn 131

to enable health programme leads and digital health teams (including software developers) to have a joint understanding of the health content within the digital system, through a transparent mechanism to review the validity and accuracy of the health content; and

### Nguồn gốc: Objectives — đoạn 132

»

### Nguồn gốc: Objectives — đoạn 133

to provide a starting point of the core data elements and decision-support logic that should be included within DTDS systems for ANC.

### Nguồn gốc: Objectives — đoạn 134

Information detailed in this DAK reflects generic workflow processes, data and decision-support algorithms, as derived from the 2016 WHO recommendations on antenatal care for a positive pregnancy experience (12) and other related WHO documents described below. This DAK also includes technical considerations for self-care interventions from the perspective of the health worker who promotes these interventions to a client. In addition, this DAK describes linkages to related services for ANC, such as the identification and management of intimate partner violence (IPV) and considerations for adolescents. Note that the outputs of the DAKs are intentionally generic and will need to be contextualized to local policies and requirements.

### Nguồn gốc: Objectives — đoạn 135

DAKs have also been developed for family planning, HIV and STIs, and this approach is being expanded to additional health domains, such as immunizations, postnatal care (PNC), and child health. To complement these there is a forthcoming DAK for self-care interventions from the perspective of a client; taken together, all of these DAKs work towards a comprehensive approach for standardized software requirements for primary health care settings.

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 136

The DAK comprises eight interlinked components: (1) health interventions and associated recommendations; (2) generic personas; (3) user scenarios; (4) generic business processes and workflows; (5) core data elements; (6) decision-support logic; (7) indicators and reporting requirements; and (8) highlevel functional and non-functional requirements. Table 1 provides an overview of each of the contributing components of the DAK, which this document elaborates. All information within the adaptation kit represents a generic starting point, which can then be adapted according to the specific context.

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 137

Furthermore, this DAK builds on previous requirements, gathering work conducted by PATH in Common requirements for maternal health information systems (13) by leveraging the developed workflows and elaborating on the data and decision-support requirements.

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 138

Table 1. Components of a digital adaptation kit

### Nguồn gốc: 1. Health — đoạn 139

interventions and

### Nguồn gốc: recommendations included within this digital — đoạn 140

adaptation kit (DAK). DAKs are meant to be a repackaging and integration of WHO guidelines and guidance documents in a particular health domain. The list of health interventions is drawn from the universal health coverage (UHC) menu of interventions compiled by WHO (14).

### Nguồn gốc: To understand how — đoạn 141

this DAK would be applied to a digital tracking and decision-support system in the context of specific health programmes and interventions

### Nguồn gốc: To understand how — đoạn 142

» List of related health interventions based on WHO’s

### Nguồn gốc: UHC essential — đoạn 143

interventions

### Nguồn gốc: UHC essential — đoạn 144

» Contextualization to reflect current or planned national policies

### Nguồn gốc: Depiction of the end-users, supervisors and related — đoạn 145

stakeholders who would be interacting with the digital system or involved in the care pathway.

### Nguồn gốc: To understand the — đoạn 146

wants, needs and constraints of the end-users

### Nguồn gốc: To understand the — đoạn 147

» Description, competencies and essential interventions performed by targeted personas

### Nguồn gốc: A local adaptation of the personas should contain — đoạn 148

high-level information to describe the provider of the health service (e.g. the general background, roles and responsibilities, motivations, challenges, and environmental factors).

### Nguồn gốc: Overview — đoạn 149

» List of related WHO

### Nguồn gốc: recommendations — đoạn 150

based on guidelines and guidance documents » Greater specification and details on the endusers based on real people (i.e. health workers) in a given context

### Nguồn gốc: recommendations — đoạn 151

overview

### Nguồn gốc: Components of a digital adaptation kit — đoạn 152

overview

### Nguồn gốc: Narratives that describe how the different — đoạn 153

personas may interact with each other.

### Nguồn gốc: To understand how — đoạn 154

the system would be used, and how it would fit into existing workflows

### Nguồn gốc: To understand how — đoạn 155

» Example narrative of how the targeted personas may interact with each other during a workflow

### Nguồn gốc: To understand how — đoạn 156

» Greater specification and details on the real needs of end-users in a given context

### Nguồn gốc: Contextualization — đoạn 157

and system design –

### Nguồn gốc: To understand how — đoạn 158

the digital system would fit into existing workflows and how best to design the system for that

### Nguồn gốc: purpose — đoạn 159

» Overview matrix presenting the key processes in antenatal care (ANC)

### Nguồn gốc: purpose — đoạn 160

» Customization of the workflows that can include additional forks, alternative pathways or entirely new workflows

### Nguồn gốc: System design and — đoạn 161

interoperability – To know which data elements need to be logged and how they map to other standard terminologies (e.g.

### Nguồn gốc: Medicine [SNOMED]) — đoạn 162

for interoperability with other standardsbased systems

### Nguồn gốc: Medicine [SNOMED]) — đoạn 163

» List of data elements

### Nguồn gốc: The user scenarios are only illustrative and — đoạn 164

intended to give an idea of a typical workflow.

### Nguồn gốc: 4. Generic — đoạn 165

business processes and workflows

### Nguồn gốc: A business process is a set of related activities or — đoạn 166

tasks performed together to achieve the objectives of the health programme area, such as registration, counselling, referrals (1,15).

### Nguồn gốc: 5. Core data — đoạn 167

elements

### Nguồn gốc: Data elements required throughout the different — đoạn 168

points of the workflow.

### Nguồn gốc: Workflows are a visual representation of the — đoạn 169

progression of activities (tasks, decision points, interactions) that are performed within the business process (1,16).

### Nguồn gốc: International Classification of Diseases version — đoạn 170

11 (ICD-11) codes and other established concept mapping standards to ensure the data dictionary is compatible with other digital systems.

### Nguồn gốc: International Classification of Diseases version — đoạn 171

» Workflows for identified business processes with annotations

### Nguồn gốc: International Classification of Diseases version — đoạn 172

» Link to data dictionary with detailed data specifications in spreadsheet format (see Web Annex A).

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 173

» Translation of “data labels” into the local language and additional data elements created depending on the context

### Nguồn gốc: 6. Decision-support — đoạn 174

logic

### Nguồn gốc: Decision-support logic and algorithms to support — đoạn 175

appropriate service delivery in accordance with WHO clinical, public health and data use guidelines.

### Nguồn gốc: System design — đoạn 176

and adherence to recommended clinical practice – To know what underlying logic needs to be coded into the system

### Nguồn gốc: System design — đoạn 177

» List of decisions that need to be made throughout the encounter

### Nguồn gốc: System design — đoạn 178

» Change of specific thresholds or triggers in a logic (IF/THEN) statement, e.g. body mass index (BMI) cut-off, age trigger for “youth friendly” services

### Nguồn gốc: System design — đoạn 179

» Link to decisionsupport tables in a spreadsheet format with inputs, outputs and triggers for each decision-support logic (see Web

### Nguồn gốc: Annex B) — đoạn 180

» Scheduling logic for services

### Nguồn gốc: 7. Indicators and — đoạn 181

performance metrics

### Nguồn gốc: 8. Functional and — đoạn 182

non-functional requirements

### Nguồn gốc: Core set of indicators that need to be aggregated — đoạn 183

for decision-making, performance metrics, and subnational and national reporting.

### Nguồn gốc: These indicators and metrics are based on data — đoạn 184

that can feasibly be captured from a routine digital system, rather than survey-based tools.

### Nguồn gốc: List of core functions and capabilities the system — đoạn 185

must have to meet the end-users’ needs and achieve tasks within the business process.

### Nguồn gốc: List of core functions and capabilities the system — đoạn 186

» Additional decision-support logic formulas depending on the context

### Nguồn gốc: System design — đoạn 187

and adherence to recommended health monitoring practices – To know what calculations and secondary data use is needed for the system, based on the principle of “collect once, use many” (10)

### Nguồn gốc: System design — đoạn 188

» Indicators table with numerator and denominator of data elements for calculation, along with appropriate disaggregation (See Web Annex C)

### Nguồn gốc: System design — đoạn 189

» Changing calculation formulas of indicators

### Nguồn gốc: To know what the — đoạn 190

system should be able to do

### Nguồn gốc: To know what the — đoạn 191

» Table of functional and non-functional requirements with the intended end-user of each requirement, as well as why that user needs that functionality in the system (See Web

### Nguồn gốc: Annex D) — đoạn 192

» Adding or reducing functions and system capabilities based on budget and end-user needs and preferences

### Nguồn gốc: Annex D) — đoạn 193

» Adding indicators » Changing the definition of the primary data elements used to calculate the indicator based on data available

### Nguồn gốc: Annex D) — đoạn 194

overview

### Nguồn gốc: Component — đoạn 195

overview

### Nguồn gốc: Component — đoạn 196

BOX 2

### Nguồn gốc: Notation guidance — đoạn 197

Throughout the DAK, there are identification (ID) numbers to simplify tracking and referencing of each of the components. Note that the DAK represents an overview across the different components, while the comprehensive and complete outputs of each component (e.g. data dictionary, decision-support tables) are included in appended spreadsheets. The notation guidance is as follows.

### Nguồn gốc: Component 4: Business processes and workflows — đoạn 198

Each workflow should have a “Process name” and a corresponding letter »

### Nguồn gốc: Component 4: Business processes and workflows — đoạn 199

»

### Nguồn gốc: Component 4: Business processes and workflows — đoạn 200

Each workflow should also have a “Process ID” that should be structured “Abbreviated health domain” (e.g. ANC). “Corresponding letter for the process” (e.g. A) Each activity in the workflow should be numbered with an “Activity ID” that should be structured “Process ID” from above “Activity Number” e.g. ANC.B7

### Nguồn gốc: Component 5: Core data elements (data dictionary) — đoạn 201

Each data element should have a running number and a “Data Element (DE) ID” that should be structured “Abbreviated health domain” (e.g.

### Nguồn gốc: Component 6: Decision-support logic — đoạn 202

Each decision-support logic table should have a running number and a “Decision-support table (DT) ID” that should be structured “Abbreviated health domain” (e.g. ANC).“DT”.“Sequential number of the decision-support table” (e.g. ANC.DT.1, ANC.DT.2)

### Nguồn gốc: Component 7: Indicators and performance metrics — đoạn 203

Each indicator should have an “Indicator ID” that should be structured “Abbreviated health domain” (e.g. ANC).“IND”.“Sequential number of the indicator”(e.g. ANC.IND.1, ANC.IND.2)

### Nguồn gốc: Component 8: High-level system requirements — đoạn 204

»

### Nguồn gốc: Each functional requirement should have a “Functional requirement — đoạn 205

ID” that should be structured “Abbreviated health domain” (e.g. ANC).“REQ”.“Sequential number of the functional requirement”(e.g.

### Nguồn gốc: ANC.REQ.1, ANC.REQ.1) — đoạn 206

»

### Nguồn gốc: Each non-functional requirement should have a “Non-functional — đoạn 207

requirement ID” that should be structured “Abbreviated health domain” (e.g. ANC).“NFXNREQ”.“Sequential number of non-functional requirements” (e.g ANC.NFXNREQ.1, ANC. NFXNREQ.2)

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 208

overview

### Nguồn gốc: Target audience — đoạn 209

The primary target audience for this DAK is health programme managers within the ministry of health (MOH), who will be working with their digital or health information systems counterparts in determining the health content requirements for an ANC DTDS system. The health programme manager is responsible for overseeing and monitoring the implementation of the clinical practices and policies for the health programme area, in this case ANC.

### Nguồn gốc: Target audience — đoạn 210

The DAK also equips individuals responsible for translating health-system processes and guidance documents for use within digital systems with the necessary components to kick-start the process of developing a DTDS system in a standards-compliant manner. These individuals are also known as business analysts who interface between health content experts and software development teams. Specifically, the adaptation kit contains key outputs, such as the data dictionary and decision-support algorithms, to ensure the validity and consistency of the health content with the DTDS system.

### Nguồn gốc: Target audience — đoạn 211

Additionally, using this DAK requires a collaboration between health programme managers and counterparts in digital health and health information systems. Although each DAK focuses on a particular health programme area (in this case ANC), the DAKs are envisioned to be used in a modular format and link to other health programme areas within primary health care settings, in an effort to support integration across services. For example, WHO will be releasing a DAK for family planning and is also planning the development of a PNC DAK to be released following the publication of the PNC guidelines, to ensure care across the sexual and reproductive health continuum, along with DAKs for other health areas.

### Nguồn gốc: Scenarios for using the DAK — đoạn 212

The DAK may be used across various scenarios, some of which are listed below. Scenario 1:

### Nguồn gốc: Incorporating WHO — đoạn 213

guideline content into existing digital tracking and decision-support systems

### Nguồn gốc: Overview — đoạn 214

Countries that already have digital systems in place, such as electronic medical records (EMRs) and decision-support tools, may use the information in the DAK to cross-check whether the underlying content and data for specific health programme areas, such as ANC or family planning, are aligned to WHO guidelines. Users of the DAK can identify and extract specific decision algorithms that would need to be incorporated into their existing digital systems. By reviewing this systematic documentation, health programme managers and implementers can more readily identify differences in workflows, data inputs and decision-support logics to examine the rationale for deviations and understanding local adaptions of guideline content.

### Nguồn gốc: Overview — đoạn 215

overview

### Nguồn gốc: Overview — đoạn 216

Scenario 2:

### Nguồn gốc: Transitioning from paper — đoạn 217

to digital tracking and decision-support systems

### Nguồn gốc: Transitioning from paper — đoạn 218

Some countries may currently have paper-based systems that they would like to digitize. The process of optimizing paper-based client-level systems into digital records and decision support may be overwhelming. Users in this scenario may review the DAK as a starting point for streamlining the necessary data elements and decision support that should be in the optimized client-level digital system. Users may also then refer to the paper-based tools to determine whether there are missing fields or content that should also be included in the digital system.

### Nguồn gốc: Transitioning from paper — đoạn 219

Additionally, users should also review the WHO Handbook for digitizing primary health care (17), which provides stepwise guidance on how to map data on paper-based forms into a digital system, including ways of accounting for data elements that are redundant or may not add value to the health system.

### Nguồn gốc: Transitioning from paper — đoạn 220

Scenario 3:

### Nguồn gốc: Linking aggregate HMIS — đoạn 221

(e.g. DHIS2) to digital tracking and decisionsupport systems used at point of care

### Nguồn gốc: Linking aggregate HMIS — đoạn 222

In some instances, countries may already have a digital system for aggregate reporting and HMIS, but may not yet have implemented digital systems that function at the service-delivery level. The DAK can guide the development of a digital client record system that operates at point of care, and ensure that there are linkages between the aggregate and service-delivery levels (e.g. community or facility level).

### Nguồn gốc: Linking aggregate HMIS — đoạn 223

As such, a component of the DAK provides aggregate indicators derived from individual-level data to provide the linkage between these different levels. Complementary guidance dedicated specifically to aggregate-level data, such as Analysis and use of facility: guidance for RMNCAH [reproductive, maternal, newborn, child and adolescent health] programme managers (18), should also be consulted for supporting the use of routine data at the facility management and district levels.

### Nguồn gốc: Linking aggregate HMIS — đoạn 224

This DAK includes data elements mapped to ICD codes, and other standards, to support the design of interoperable Scenario 4: Leveraging data standards systems. The data dictionary in the Web Annex A provides the necessary codes for different data elements, thus to promote interoperability reducing the time for implementers to incorporate these global standards into the design of their digital systems.

### Nguồn gốc: Linking aggregate HMIS — đoạn 225

and integrated systems

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 226

overview

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 227

Scenario 5:

### Nguồn gốc: WHO ANC digital module — đoạn 228

WHO will be releasing a reference ANC digital module as a decision-support and digital tracking tool that reflects the ANC DAK content to assist countries in identifying which data elements and decision-support logic will need to be modified for their context.

### Nguồn gốc: WHO ANC digital module — đoạn 229

In addition, a critical part of service delivery in any health domain relies on engaging with clients, for example, pregnant women in the ANC domain. Digital interventions aimed at clients themselves, inclusive of pregnant women, such as on-demand information services, targeted client communication (e.g. transmitting health information and reminders), reporting of health-system feedback by clients on the quality of care, accessing their own medical records/home-based records, and self-monitoring of their health and diagnostic data (8), are all emerging approaches for complementing the services provided by health workers. The content requirements for these client-facing digital tools will be detailed in a forthcoming self-care interventions DAK.

### Nguồn gốc: Assumptions — đoạn 230

The use of this DAK for ANC is also based on the following critical assumptions. »

### Nguồn gốc: Assumptions — đoạn 231

The woman’s pregnancy is confirmed, and she intends to proceed with the pregnancy.

### Nguồn gốc: Assumptions — đoạn 232

»

### Nguồn gốc: Assumptions — đoạn 233

ANC contact is being provided in the context of routine services at the primary care level.

### Nguồn gốc: Assumptions — đoạn 234

»

### Nguồn gốc: Assumptions — đoạn 235

Content for the ANC contact is intended for routine services in primary health care (i.e. not for specialized services).

### Nguồn gốc: Assumptions — đoạn 236

»

### Nguồn gốc: Assumptions — đoạn 237

Local adaptations will be made to this DAK, as this document is intended to reflect commonalities generalized from different settings (the content is assumed to be 80% generic for use across different settings and 20% of the content will require local contextualization).

### Nguồn gốc: Assumptions — đoạn 238

»

### Nguồn gốc: Assumptions — đoạn 239

National policy makers for ANC plan to or have already adopted and adapted the WHO ANC guideline WHO recommendations on antenatal care for a positive pregnancy experience to the local context (19). For countries that are yet to adopt and adapt the 2016 WHO ANC guideline, the contents of the DAK can support the transition to the recommended interventions by presenting them in a format that can be digitized.

### Nguồn gốc: Assumptions — đoạn 240

For the purposes of this DAK, the terms “woman” and “client” will be used interchangeably to denote the individual seeking ANC services. The term “client” will be used for general processes, such as registration, that may apply for other health areas; the term “woman”or “pregnant woman” will be used in contexts of ANC-specific processes.

### Nguồn gốc: Overview — đoạn 241

overview

### Nguồn gốc: DAKs represent one — đoạn 242

resource in the broader digital health ecosystem and should be used once there is a strategic vision by the MOH to use a DTDS system. In contexts where such a vision may not exist, users should first consult the WHO–ITU National eHealth strategy toolkit (20), WHO guidelines on digital interventions for health system strengthening (7) and the

### Nguồn gốc: WHO Digital investments — đoạn 243

and implementation guide (1) to establish a better understanding of how to select and apply appropriate digital health interventions. Fig. 3 situates DAKs within the broader set of resources for planning and implementing digital health systems.

### Nguồn gốc: WHO Digital investments — đoạn 244

FIG. 3

### Nguồn gốc: Digital adaptation kits within the broader digital — đoạn 245

health ecosystem

### Nguồn gốc: ENVIRONMENT — đoạn 246

+ Conduct an inventory of existing or previously used software applications, ICT systems and other tools to better understand the requirements for reuse and interoperability

### Nguồn gốc: PLANNING — đoạn 247

+ Develop a national digital health strategy outlining overarching needs, desired activities and outcomes + Define a vision for how the health system will be strengthened through the use of digital technology

### Nguồn gốc: WHO Digital — đoạn 248

health atlas

### Nguồn gốc: National — đoạn 249

eHealth strategy toolkit

### Nguồn gốc: National — đoạn 250

+ Formulate a digital health investment roadmap to support the national digital health strategy + Plan and identify appropriate digital interventions, alongside the health and data content, to improve health system processes and address programmatic needs

### Nguồn gốc: Digital — đoạn 251

implementation investment guide

### Nguồn gốc: REQUIREMENTS — đoạn 252

+ Identify validated health content appropriate for the implementation context + Ensure use of content aligned with identified standards for the future state

### Nguồn gốc: WHO — đoạn 253

handbook for digitalizing primary health care

### Nguồn gốc: DATA USE — đoạn 254

+ Monitor your implementation to ensure digital implementations are functioning as intended and having the desired effect + Foster data-driven adaptive change management within the overall health system

### Nguồn gốc: Monitoring — đoạn 255

and evaluating digital health interventions

### Nguồn gốc: SCALING — đoạn 256

+ Maintain and sustain digital health implementations + Identify risks and appropriate mitigations

### Nguồn gốc: Development — đoạn 257

toolkit:

### Nguồn gốc: Bank Total — đoạn 258

mHealth cost of assessment ownership and planning tool for scale

### Nguồn gốc: Digital — đoạn 259

health investment review tool

### Nguồn gốc: HIS Stages of — đoạn 260

continuous improvement

### Nguồn gốc: WHO — đoạn 261

Guideline:

### Nguồn gốc: Classification — đoạn 262

of digital health interventions

### Nguồn gốc: Principles — đoạn 263

for donor alignment

### Nguồn gốc: Principles for — đoạn 264

digital development

### Nguồn gốc: Global — đoạn 265

goods guidebook

### Nguồn gốc: Humancentred — đoạn 266

design toolkit

### Nguồn gốc: WHO Digital — đoạn 267

clearinghouse

### Nguồn gốc: Guidance — đoạn 268

for investing in digital health

### Nguồn gốc: Digital — đoạn 269

identity toolkit

### Nguồn gốc: Recommendations — đoạn 270

on Digital

### Nguồn gốc: Strengthening — đoạn 271

+ Review the current state and develop an architecture blueprint for the design of the digital health implementations + Identify validated open standards to ensure data exchange, systems integration and future-proofing of digital health implementations.

### Nguồn gốc: Exchange — đoạn 272

(Open HIE)

### Nguồn gốc: Global — đoạn 273

digital health index

### Nguồn gốc: Digital — đoạn 274

investment framework (eGov)

### Nguồn gốc: ITU Digital — đoạn 275

health platform handbook

### Nguồn gốc: MachineDigital — đoạn 276

readable adaptation recommenkits (this document) dations

### Nguồn gốc: Defining — đoạn 277

and building a data use culture

### Nguồn gốc: WHO Core — đoạn 278

indicator sets

### Nguồn gốc: Be Mobile — đoạn 279

handbooks for noncommunicable diseases

### Nguồn gốc: MEASURE — đoạn 280

data demand and use of resources

### Nguồn gốc: Online environment — đoạn 281

ICT: information and communications technology; M&E: monitoring and evaluation. Source: Adapted from WHO (1).

### Nguồn gốc: recommendations — đoạn 282

personas scenarios workflows

### Nguồn gốc: Part — đoạn 283

data

### Nguồn gốc: Digital adaptation — đoạn 284

kit content for antenatal care

### Nguồn gốc: Digital adaptation — đoạn 285

decisions indicators

### Nguồn gốc: Digital adaptation — đoạn 286

requirements

### Nguồn gốc: recommendations — đoạn 287

scenarios

### Nguồn gốc: recommendations — đoạn 288

The key interventions for routine antenatal care (ANC) are the following, as defined in the WHO UHC compendium of interventions (14):

### Nguồn gốc: recommendations — đoạn 289

requirements

### Nguồn gốc: recommendations — đoạn 290

indicators

### Nguồn gốc: recommendations — đoạn 291

decisions

### Nguồn gốc: recommendations — đoạn 292

data

### Nguồn gốc: recommendations — đoạn 293

This DAK focuses on the following health interventions and recommendations.

### Nguồn gốc: recommendations — đoạn 294

workflows

### Nguồn gốc: recommendations — đoạn 295

personas

### Nguồn gốc: Health — đoạn 296

interventions and

### Nguồn gốc: 1.1 Interventions referenced in this DAK are based on the WHO universal health coverage (U — đoạn 297

of interventions »

### Nguồn gốc: 1.1 Interventions referenced in this DAK are based on the WHO universal health coverage (U — đoạn 298

health education and counselling to promote healthy pregnancy

### Nguồn gốc: 1.1 Interventions referenced in this DAK are based on the WHO universal health coverage (U — đoạn 299

»

### Nguồn gốc: 1.1 Interventions referenced in this DAK are based on the WHO universal health coverage (U — đoạn 300

nutritional supplementation during pregnancy

### Nguồn gốc: 1.1 Interventions referenced in this DAK are based on the WHO universal health coverage (U — đoạn 301

»

### Nguồn gốc: 1.1 Interventions referenced in this DAK are based on the WHO universal health coverage (U — đoạn 302

maternal and fetal assessment and screening during pregnancy

### Nguồn gốc: 1.1 Interventions referenced in this DAK are based on the WHO universal health coverage (U — đoạn 303

»

### Nguồn gốc: 1.1 Interventions referenced in this DAK are based on the WHO universal health coverage (U — đoạn 304

preventive measures and vaccination during pregnancy

### Nguồn gốc: 1.1 Interventions referenced in this DAK are based on the WHO universal health coverage (U — đoạn 305

»

### Nguồn gốc: 1.1 Interventions referenced in this DAK are based on the WHO universal health coverage (U — đoạn 306

treatment for physiological symptoms during pregnancy

### Nguồn gốc: 1.1 Interventions referenced in this DAK are based on the WHO universal health coverage (U — đoạn 307

»

### Nguồn gốc: 1.1 Interventions referenced in this DAK are based on the WHO universal health coverage (U — đoạn 308

ANC models with a minimum of eight contacts.

### Nguồn gốc: 1.2 WHO guidelines, recommendations and guidance — đoạn 309

The DAKs are intended to reflect health recommendations and content that has already been published in WHO guidelines and guidance documents. The health content and interventions are drawn from the WHO recommendations on antenatal care for a positive pregnancy experience (2016) and additional guidance also available through the ANC portal (www.srhr.org/antenatalcare/).

### Nguồn gốc: recommendations — đoạn 310

Other guidelines represented in the DAK include:

### Nguồn gốc: recommendations — đoạn 311

»

### Nguồn gốc: recommendations — đoạn 312

Diagnostic criteria and classification of hyperglycaemia first detected in pregnancy (2013)

### Nguồn gốc: recommendations — đoạn 313

»

### Nguồn gốc: recommendations — đoạn 314

WHO recommendations on community mobilization through facilitated participatory learning and action cycles with women’s groups for maternal and newborn health (2014)

### Nguồn gốc: recommendations — đoạn 315

»

### Nguồn gốc: recommendations — đoạn 316

WHO recommendations for the prevention and management of tobacco use and second-hand smoke exposure in pregnancy (2013)

### Nguồn gốc: recommendations — đoạn 317

»

### Nguồn gốc: recommendations — đoạn 318

Guidelines for the identification and management of substance use and substance use disorders in pregnancy (2014)

### Nguồn gốc: recommendations — đoạn 319

»

### Nguồn gốc: recommendations — đoạn 320

Consolidated guidelines on HIV testing services. 5Cs: consent, confidentiality, counselling, correct results and connection (2015)

### Nguồn gốc: recommendations — đoạn 321

»

### Nguồn gốc: recommendations — đoạn 322

Guidelines for the treatment of malaria (3rd edition, 2015)

### Nguồn gốc: recommendations — đoạn 323

»

### Nguồn gốc: recommendations — đoạn 324

Guideline on when to start antiretroviral therapy and on pre-exposure prophylaxis for HIV (2015)

### Nguồn gốc: recommendations — đoạn 325

»

### Nguồn gốc: recommendations — đoạn 326

WHO recommendations on health promotion interventions for maternal and newborn health 2015

### Nguồn gốc: recommendations — đoạn 327

»

### Nguồn gốc: recommendations — đoạn 328

Consolidated guideline on sexual and reproductive health and rights of women living with HIV (WHO, UNAIDS, UNFPA, OHCHR, 2017)

### Nguồn gốc: recommendations — đoạn 329

»

### Nguồn gốc: recommendations — đoạn 330

Guideline: preventive chemotherapy to control soil-transmitted helminth infections in at-risk population groups (2017)

### Nguồn gốc: recommendations — đoạn 331

»

### Nguồn gốc: recommendations — đoạn 332

WHO guideline on syphilis screening and treatment for pregnant women (2017)

### Nguồn gốc: recommendations — đoạn 333

»

### Nguồn gốc: recommendations — đoạn 334

Guidelines on hepatitis B and C testing (2017) WHO recommendations on adolescent sexual and reproductive health and rights (2018)

### Nguồn gốc: recommendations — đoạn 335

»

### Nguồn gốc: recommendations — đoạn 336

WHO recommendations: intrapartum care for a positive childbirth experience (2018)

### Nguồn gốc: recommendations — đoạn 337

»

### Nguồn gốc: recommendations — đoạn 338

WHO consolidated guideline on self-care interventions for health (2019)

### Nguồn gốc: recommendations — đoạn 339

»

### Nguồn gốc: recommendations — đoạn 340

WHO recommendations for prevention and treatment of pre-eclampsia and eclampsia (2011); also updated in 2018 and 2020

### Nguồn gốc: recommendations — đoạn 341

scenarios

### Nguồn gốc: recommendations — đoạn 342

Systematic screening for active tuberculosis: principles and recommendations (2013)

### Nguồn gốc: recommendations — đoạn 343

personas

### Nguồn gốc: recommendations — đoạn 344

»

### Nguồn gốc: recommendations — đoạn 345

workflows data

### Nguồn gốc: recommendations — đoạn 346

Implementation and data guidance that contributed to DAK components include:

### Nguồn gốc: recommendations — đoạn 347

»

### Nguồn gốc: recommendations — đoạn 348

Responding to intimate partner violence and sexual violence against women: WHO clinical and policy guidelines (2013)

### Nguồn gốc: recommendations — đoạn 349

»

### Nguồn gốc: recommendations — đoạn 350

Pregnancy, childbirth, postpartum and newborn care: a guide for essential practice (3rd edition, 2015)

### Nguồn gốc: recommendations — đoạn 351

»

### Nguồn gốc: recommendations — đoạn 352

Managing complications in pregnancy and childbirth: a guide for midwives and doctors (2nd edition, 2017)

### Nguồn gốc: recommendations — đoạn 353

»

### Nguồn gốc: recommendations — đoạn 354

WHO/UNICEF guidance note: ensuring sustained protection against diphtheria: replacing Tetanus Toxoid with Tetanus–diphtheria vaccine (2018)

### Nguồn gốc: recommendations — đoạn 355

»

### Nguồn gốc: recommendations — đoạn 356

Analysis and use of health facility data: guidance for RMNCAH (2019)

### Nguồn gốc: recommendations — đoạn 357

»

### Nguồn gốc: recommendations — đoạn 358

Clinical management of severe acute respiratory infection (SARI) when COVID-19 disease is suspected (Interim guidance, 13 March 2020)

### Nguồn gốc: recommendations — đoạn 359

requirements

### Nguồn gốc: Health interventions and recommendations — đoạn 360

indicators

### Nguồn gốc: Health interventions and recommendations — đoạn 361

WHO recommendations: optimizing health worker roles to improve access to key maternal and newborn health interventions through task shifting (2012)

### Nguồn gốc: Health interventions and recommendations — đoạn 362

decisions

### Nguồn gốc: Health interventions and recommendations — đoạn 363

»

### Nguồn gốc: recommendations — đoạn 364

indicators requirements

### Nguồn gốc: Generic — đoạn 365

personas

### Nguồn gốc: Generic — đoạn 366

A persona is a depiction of a relevant stakeholder, or “end-user”, of the system. Although the specific roles and demographic profile of the personas will vary depending on the setting, the generic personas are based on the WHO core competencies and credentials of different health worker personas.

### Nguồn gốc: Generic — đoạn 367

Please note that these are developed based on synthesis across multiple contexts as a starting point, and further contextualization will be required according to the needs, motivations and challenges of the targeted personas in each setting.

### Nguồn gốc: 2.1 Targeted generic personas — đoạn 368

The targeted personas for this ANC DAK are skilled health-care professionals operating in primary health care settings and able to provide the essential interventions listed below. WHO and others define skilled health personnel “as competent maternal and newborn health professionals educated, trained and regulated to national and international standards. They are competent to: (i) provide and promote evidence-based, human-rights-based, quality, socio-culturally sensitive and dignified care to women and newborns;

### Nguồn gốc: 2.1 Targeted generic personas — đoạn 369

decisions

### Nguồn gốc: 2.1 Targeted generic personas — đoạn 370

data

### Nguồn gốc: 2.1 Targeted generic personas — đoạn 371

workflows

### Nguồn gốc: 2.1 Targeted generic personas — đoạn 372

scenarios

### Nguồn gốc: 2.1 Targeted generic personas — đoạn 373

personas

### Nguồn gốc: Component — đoạn 374

(ii) facilitate physiological processes during labour and delivery to ensure a clean and positive childbirth experience; and (iii) identify and manage or refer women and/or newborns with complications” (21). In the case of ANC, the occupational titles of the targeted personas include auxiliary nurse midwives (ANM), nurses and midwives, though other healthcare professionals with competencies above this level may also be included. The descriptions of these targeted health worker personas as defined by the WHO are presented in Table 2 (23).

### Nguồn gốc: Auxiliary nurse — đoạn 375

midwife (ANM)

### Nguồn gốc: Auxiliary nurse — đoạn 376

Auxiliary nurse midwives (ANMs) assist in the provision of maternal and newborn health care, particularly during childbirth but also in the prenatal and postpartum periods. ANMs have some training in secondary school and a period of on-the-job training may be included, sometimes formalized in apprenticeships. Like an auxiliary nurse, an auxiliary nurse midwife has basic nursing skills but no training in nursing decisionmaking. They possess some competencies in midwifery but are not fully qualified as midwives (22).

### Nguồn gốc: Indonesia) — đoạn 377

3221 (Nursing associate professional)

### Nguồn gốc: Midwife — đoạn 378

A person who has been assessed and registered by a state midwifery regulatory authority or similar regulatory authority. They offer care to childbearing women during pregnancy, labour and birth, and during the postpartum period. They also care for the newborn and assist the mother with breastfeeding.

### Nguồn gốc: Midwife — đoạn 379

Their education lasts three, four or more years in nursing school, and leads to a university or postgraduate university degree, or the equivalent. A registered midwife has the full range of midwifery skills (22).

### Nguồn gốc: Registered midwife, midwife, — đoạn 380

community midwife

### Nguồn gốc: Registered midwife, midwife, — đoạn 381

2222 (Midwifery professional)

### Nguồn gốc: Nurse — đoạn 382

A graduate who has been legally authorized (registered) to practise after examination by a state board of nurse examiners or similar regulatory authority. Education includes three, four or more years in nursing school, and leads to a university or postgraduate university degree, or the equivalent. A registered nurse has the full range of nursing skills.

### Nguồn gốc: Registered nurse, nurse — đoạn 383

practitioner, clinical nurse specialist, advance practice nurse, practice nurse, licensed nurse, diploma nurse, nurse clinician

### Nguồn gốc: Registered nurse, nurse — đoạn 384

2221 (Nursing professional)

### Nguồn gốc: Registered nurse, nurse — đoạn 385

personas

### Nguồn gốc: recommendations — đoạn 386

Table 2. Descriptions of key generic personas

### Nguồn gốc: recommendations — đoạn 387

3222 (Midwifery associate professional)

### Nguồn gốc: recommendations — đoạn 388

scenarios workflows data decisions indicators

### Nguồn gốc: recommendations — đoạn 389

requirements

### Nguồn gốc: recommendations — đoạn 390

personas

### Nguồn gốc: recommendations — đoạn 391

In addition to the targeted personas detailed above, there may be value in exploring other cadres and personas within the context of

### Nguồn gốc: ANC services, such as associate clinicians and obstetricians. However, — đoạn 392

these were not identified as the central personas for the data and decision-support content detailed in this DAK. Additional personas related to the role of the targeted nurse midwife are listed in Table 3.

### Nguồn gốc: ANC services, such as associate clinicians and obstetricians. However, — đoạn 393

FIG. 4

### Nguồn gốc: Overview of the different personas — đoạn 394

that may be involved in the delivery of antenatal care services

### Nguồn gốc: Overview of the different personas — đoạn 395

data

### Nguồn gốc: Overview of the different personas — đoạn 396

workflows

### Nguồn gốc: 2.2 Related personas — đoạn 397

scenarios

### Nguồn gốc: 2.2 Related personas — đoạn 398

Fig. 4 is an overview of the different personas that may be involved in the delivery of ANC services, with the addition of obstetrician/ gynaecologist specialist doctors. The data and decision-support content within this DAK focuses on the competencies and recommended interventions for ANMs, nurses and midwives.

### Nguồn gốc: 2.2 Related personas — đoạn 399

requirements

### Nguồn gốc: 2.2 Related personas — đoạn 400

indicators

### Nguồn gốc: 2.2 Related personas — đoạn 401

decisions

### Nguồn gốc: 2.2 Related personas — đoạn 402

Source: WHO (2012) (22).

### Nguồn gốc: ISCO Code — đoạn 403

(if relevant) (23)

### Nguồn gốc: Pregnant woman — đoạn 404

Pregnant women are the primary clients receiving antenatal care (ANC) services from the targeted health worker personas. While this is a diverse population group with different demographics, psychological and social needs, they generally have the following expectations from ANC programmes:

### Nguồn gốc: N/A — đoạn 405

personas

### Nguồn gốc: recommendations — đoạn 406

Table 3. Additional personas related to the role of the targeted nurse midwife

### Nguồn gốc: recommendations — đoạn 407

» maintaining a healthy pregnancy for mother and baby (including preventing and treating risks, illness and death); » having an effective transition to positive labour and birth; » maintaining physical and sociocultural normality;

### Nguồn gốc: recommendations — đoạn 408

scenarios

### Nguồn gốc: recommendations — đoạn 409

» achieving positive motherhood (including maternal self-esteem, competence, autonomy) (12,24). Specific considerations in health service delivery will need to be incorporated for pregnant women and girls from certain population groups, including, but not limited to: adolescent girls and young women, women and girls living with HIV, and women and girls with poor access to health-care facilities.

### Nguồn gốc: recommendations — đoạn 410

workflows

### Nguồn gốc: recommendations — đoạn 411

The content specifications for pregnant women will become even more important as additional client-side digital functionalities (e.g. on-demand information services, targeted client communication [reminders], reporting of health system feedback by clients on the quality of care, personal health tracking) are incorporated.

### Nguồn gốc: recommendations — đoạn 412

3259 (Health associate professionals not elsewhere classified)

### Nguồn gốc: Community health — đoạn 413

worker

### Nguồn gốc: Community health — đoạn 414

Community health workers provide health education, referral and follow-up; case management and basic preventive health care; and home visiting services to specific communities. They provide support and assistance to pregnant women and their families in navigating the health and social services system (23).

### Nguồn gốc: Health extension worker — đoạn 415

3253 (Community health workers)

### Nguồn gốc: Nurse/Midwife — đoạn 416

supervisor

### Nguồn gốc: Nurse/Midwife — đoạn 417

Nurse/midwife supervisors coordinate and manage the nurses and midwives within their catchment area, including through a review of their monthly reports and disseminating clinical protocols.

### Nguồn gốc: Facility manager — đoạn 418

Facility managers plan, direct, coordinate and evaluate the provision of clinical and community health services at the health-care facility. They provide overall direction, policy standards and operational criteria for the units they manage, including supervising and evaluating the recruitment, training and work activities of personnel. They monitor the use of health services and resources at the health-care facility. They liaise with other health and welfare service providers, boards (including community boards) and funding bodies to coordinate the provision of services.

### Nguồn gốc: Health facility administrator — đoạn 419

1342 (Health service managers)

### Nguồn gốc: Health facility administrator — đoạn 420

indicators

### Nguồn gốc: Community health volunteer, — đoạn 421

village health worker, treatment supporter, promotors, etc.

### Nguồn gốc: Community health volunteer, — đoạn 422

N/A: not applicable.

### Nguồn gốc: Community health volunteer, — đoạn 423

requirements

### Nguồn gốc: Generic personas — đoạn 424

decisions

### Nguồn gốc: Generic personas — đoạn 425

Any health worker who performs functions related to health-care delivery, was trained in some way in the context of the intervention but has received no formal professional or paraprofessional certificate or tertiary education degree (22).

### Nguồn gốc: Generic personas — đoạn 426

data

### Nguồn gốc: recommendations — đoạn 427

personas scenarios

### Nguồn gốc: recommendations — đoạn 428

Although this section provides an overview of the generic roles of the targeted personas, it is important to contextualize these personas to the specific setting. The generic personas described above can be supplemented by reflecting on the following additional considerations.

### Nguồn gốc: recommendations — đoạn 429

»

### Nguồn gốc: recommendations — đoạn 430

Background and demographics (e.g. gender, age, whether they are from the community, familiarity with digital devices, do they own a mobile phone/smartphone).

### Nguồn gốc: recommendations — đoạn 431

»

### Nguồn gốc: recommendations — đoạn 432

Local environment and any relevant contextual information about their surroundings (e.g. work-site characteristics; rural or urban; availability of electricity, water, Internet; distance from nearest referral facility).

### Nguồn gốc: recommendations — đoạn 433

»

### Nguồn gốc: recommendations — đoạn 434

Expected roles and responsibilities: What are the expected roles and responsibilities based on country context? How does this differ from the roles and responsibilities defined by WHO?

### Nguồn gốc: recommendations — đoạn 435

»

### Nguồn gốc: recommendations — đoạn 436

Actual roles and responsibilities: What are their actual roles and responsibilities, if there is any difference from what is expected?

### Nguồn gốc: recommendations — đoạn 437

»

### Nguồn gốc: recommendations — đoạn 438

Context: What is the level of Internet connectivity? How are they compensated? How far away is the nearest referral facility? What other personas/ health workers do they interact with?

### Nguồn gốc: recommendations — đoạn 439

»

### Nguồn gốc: Challenges: What are the day-to-day challenges the end-user might face? — đoạn 440

»

### Nguồn gốc: Challenges: What are the day-to-day challenges the end-user might face? — đoạn 441

Motivations: What does success look like to them? Are there targets they need to achieve?

### Nguồn gốc: Challenges: What are the day-to-day challenges the end-user might face? — đoạn 442

See Annex 1 for examples of contextualized personas. For more details on persona development, please refer to the WHO Handbook for digitalizing primary health care (17).

### Nguồn gốc: Challenges: What are the day-to-day challenges the end-user might face? — đoạn 443

requirements

### Nguồn gốc: Challenges: What are the day-to-day challenges the end-user might face? — đoạn 444

indicators

### Nguồn gốc: Challenges: What are the day-to-day challenges the end-user might face? — đoạn 445

decisions

### Nguồn gốc: Challenges: What are the day-to-day challenges the end-user might face? — đoạn 446

data

### Nguồn gốc: Challenges: What are the day-to-day challenges the end-user might face? — đoạn 447

workflows

### Nguồn gốc: User — đoạn 448

scenarios

### Nguồn gốc: User — đoạn 449

personas

### Nguồn gốc: Component — đoạn 450

scenarios workflows

### Nguồn gốc: Component — đoạn 451

User scenarios are a narrative description of how different personas would interact with each other. The user scenario is provided to help the reader better understand how the system will be used and how it would fit into existing workflows. The following illustrative examples provide scenarios that may be common within ANC. In the subsequent component on workflows, these types of scenarios will be presented in a visual diagram, as opposed to narrative form. Note: these scenarios are not exhaustive and are only intended to contextualize the workflows in Component 4.

### Nguồn gốc: Component — đoạn 452

data decisions indicators

### Nguồn gốc: Component — đoạn 453

requirements

### Nguồn gốc: recommendations — đoạn 454

personas scenarios workflows data

### Nguồn gốc: Registration Clerk: Abraham — đoạn 455

After missing her last menstrual cycle and feeling nauseous, Charity seeks care at the nearby government health centre. This is Charity’s first time coming to this health-care facility, and the clerk (Abraham) confirms that they have no record of her on file. He asks her to provide him with basic demographic information, including her date of birth, alternative contact information, and address, to register Charity into the system and provide her with a QR code that she can use in case she returns to the facility for her following ANC appointments.

### Nguồn gốc: Registration Clerk: Abraham — đoạn 456

Once Charity is fully registered, she waits for a nurse (Jane) to call her into the counselling room. While Jane closes the door and lowers the shade on the window to provide privacy, Jane begins to ask questions regarding Charity’s reason for coming to the facility, as well as the date of her last menstrual period (LMP). Charity is unsure of the exact date but recalls that it was around the new year holiday. Jane administers a test to confirm Charity’s pregnancy. Upon confirming the positive pregnancy test, Jane proceeds to ask more detailed questions on Charity’s occupation, behaviours (such as smoking and caffeine intake), general health status, and obstetric history. This process could also include questions on support systems (such as family, women’s groups, financial or nutritional assistance). Based on the information Charity provides regarding her LMP, Jane is not able to conclud

### Nguồn gốc: Registration Clerk: Abraham — đoạn 457

e the exact gestational age but estimates her to be between 12 and 15 weeks. Jane lets Charity know that she would need an ultrasound as soon as possible, before her 24th week of pregnancy, to better estimate her gestational age and due date.

### Nguồn gốc: Registration Clerk: Abraham — đoạn 458

After recording Charity’s background information in the digital system, Jane asks additional questions about any current symptoms. Jane also records Charity’s weight and height measurements and conducts a physical exam, including taking her blood pressure to check that it is within the normal range. As Jane is recording these results in the digital system, she receives prompts to make sure she is providing the appropriate counselling and action. These prompts can also include reminders such as treating all pregnant women respectfully and without judgement, regardless of background or health status. Jane also orders any required additional tests such as those for diabetes, hepatitis and HIV, being sure to inform Charity about all the tests being done and to answer any questions she has about them.

### Nguồn gốc: Registration Clerk: Abraham — đoạn 459

Jane completes the counselling and advises Charity to reduce her caffeine intake and use a condom as she is still at risk from sexually transmitted infections (STIs) while pregnant. Jane also provides Charity with a supply of iron and folic acid (IFA) tablets to take daily. Jane also discusses different options for managing symptoms (e.g. nausea, lower back pain) as well as how to recognise danger signs that require contacting a health-care facility right away; Jane gives this information in a manner that encourages Charity to feel confident about making informed decisions about her and her baby’s health. After checking whether Charity has any questions, Jane schedules the next ANC contact based on the suggested dates proposed by the digital system. Since there is no ultrasound at Jane’s facility, Jane also provides Charity with a referral slip so that she can get her ultrasound done at the imaging centre before she returns for her next contact. Charity will also receive a text (SMS) reminder (assuming she has given consent during her registration) ahead of her next scheduled contact.

### Nguồn gốc: Corresponding — đoạn 460

business processes (see Component 4)

### Nguồn gốc: Corresponding — đoạn 461

This scenario refers to the following business processes:

### Nguồn gốc: B. Routine ANC contact — đoạn 462

requirements

### Nguồn gốc: B. Routine ANC contact — đoạn 463

indicators

### Nguồn gốc: B. Routine ANC contact — đoạn 464

decisions

### Nguồn gốc: Registration Clerk: Mary — đoạn 465

personas

### Nguồn gốc: Registration Clerk: Mary — đoạn 466

Charity returns to the health centre within two weeks of her previous contact, which was not planned according to the system. Charity gives the clerk (Mary) the card with the QR code that she was given at her last contact. Mary can search for and find Charity’s record, but immediately sees that she is too weak to stand up on her own. She calls the nurse midwife (Amina) for assistance so that she can begin checking to see what is wrong. Charity explains to Amina that she has had heavy vaginal bleeding over the past week. Amina performs a rapid evaluation of Charity’s general condition (vital signs, blood loss, etc.). Based on these findings, she decides to conduct further clinical examination and initial treatment at this facility. She also asks Mary to arrange for a local taxi driver to take Charity – in case a referral is necessary.

### Nguồn gốc: Registration Clerk: Mary — đoạn 467

This scenario refers to the following business processes:

### Nguồn gốc: Registration Clerk: Mary — đoạn 468

scenarios

### Nguồn gốc: Corresponding — đoạn 469

business processes (see Component 4)

### Nguồn gốc: C. Referral — đoạn 470

workflows data decisions indicators

### Nguồn gốc: C. Referral — đoạn 471

requirements

### Nguồn gốc: recommendations — đoạn 472

personas scenarios workflows data

### Nguồn gốc: Registration Clerk: Aslam Bibi — đoạn 473

Adolescent Manideepa, who is 18 years old, comes to the health-care facility accompanied by her mother-in-law. The registration clerk (Aslam Bibi) asks her for her date of birth. Once the clerk registers her date of birth, the digital system automatically calculates that Manideepa is 18 years old and prompts Aslam Bibi. The registration clerk takes note of this and discusses this with the health worker later. He enrols her as a new client. Once Mandeepa and her mother-in-law enter the examination room, the health worker closes the door and lowers the shade on the window to maintain visual and auditory privacy. As Suptapa, the health worker, opens Manideepa’s record on her password-protected tablet, she receives a pop-up message reminding her that Manideepa is an adolescent client and that she should conduct the home, education, activities/employment, drugs, suicidality and sex (HEADSS) assessment.

### Nguồn gốc: Registration Clerk: Aslam Bibi — đoạn 474

After an initial encounter with Manideepa and her mother-in-law, the health worker asks the mother-in-law to leave the examination room so she can develop rapport with Manideepa. Sutapa has forgotten the key components of a “HEADSS assessment”, so she clicks on an information icon with the pop-up message that contains some information on the types of questions she should ask. In accordance to the HEADSS assessment, the health worker asks Manideepa a series of questions starting with non-threatening questions. To help build rapport, she asks Manideepa who she lives with, how is school, and what activities she does in her free time. Sutapa obtains consent to conduct a physical exam.

### Nguồn gốc: Registration Clerk: Aslam Bibi — đoạn 475

She then asks more clinically important questions, such as when her last menstrual period was, about her past pregnancy or obstetric history, whether she suffers from any chronic illnesses, whether she was vaccinated when she was young (including for Human papillomavirus (HPV)), whether she has had any laboratory tests and whether she takes any medication. Then Suptapa conducts the physical exam. She checks Manideepa’s height, weight, blood pressure and pulse rate. Then the health worker examines the height of the uterus (symphysis-fundal height) and finds Manideepa to be about four months pregnant. Suptapa advises her to have an ultrasound scan as soon as possible and then provides the first dose tetanus toxoid-containing vaccination (TTCV). She gives Manideepa iron and folic acid and calcium tablets, explains about good food and nutrition, and asks her to sleep under an insecticide-treated bednet.

### Nguồn gốc: Registration Clerk: Aslam Bibi — đoạn 476

She also describes the options for self-managing symptoms (such as constipation, pelvic pain) and how to decide when to contact a health-care facility if the symptoms persist or if danger signs arise. Then Suptapa asks Manideepa to come for a follow-up contact as per the schedule prescribed in the guidelines. She then enquires whether Manideepa would like to receive a text reminder for the contacts. She also explains that because of Manideepa’s young age she should give birth at a health-care facility and not at home. Sutapa also takes the opportunity to counsel Manideepa on other critical components of an ANC and discusses the options for postpartum family planning, birth preparedness, and breastfeeding. Sutapa logs all of this information into her digital tool so she can remember what counselling Manideepa will need the next time she sees her.

### Nguồn gốc: Corresponding — đoạn 477

business processes (see Component 4)

### Nguồn gốc: Corresponding — đoạn 478

This scenario refers to the following business processes:

### Nguồn gốc: B. Routine ANC contact — đoạn 479

requirements

### Nguồn gốc: B. Routine ANC contact — đoạn 480

indicators

### Nguồn gốc: B. Routine ANC contact — đoạn 481

decisions

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 482

scenarios

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 483

For example, the interpretation of the user scenario for an adolescent client (3.3) is shown in Table 4. Table 4. Interpretation of a user scenario

### Nguồn gốc: Decision-support logic to be — đoạn 484

embedded

### Nguồn gốc: Functional and non-functional — đoạn 485

requirements

### Nguồn gốc: Adolescent-specific considerations which — đoạn 486

should be “triggered” if the client’s entered age is 19 years or younger

### Nguồn gốc: Adolescent-specific considerations which — đoạn 487

» Date of birth

### Nguồn gốc: Adolescent-specific considerations which — đoạn 488

» Calculation of age based on date of birth

### Nguồn gốc: Adolescent-specific considerations which — đoạn 489

» Password protection

### Nguồn gốc: Adolescent-specific considerations which — đoạn 490

» Creating a private environment

### Nguồn gốc: Adolescent-specific considerations which — đoạn 491

» Decision-support logic to trigger certain pop-ups and reminders based on the client’s age

### Nguồn gốc: Adolescent-specific considerations which — đoạn 492

» User management (login system) » Pop-up messages (“toaster messages”) and reminders

### Nguồn gốc: Adolescent-specific considerations which — đoạn 493

» Home, education, activities/employment, drugs, suicidality and sex (HEADSS) assessment guidance

### Nguồn gốc: Adolescent-specific considerations which — đoạn 494

» New client or returning client » Marital status » Past pregnancy or obstetric history » History of chronic illnesses

### Nguồn gốc: Adolescent-specific considerations which — đoạn 495

» HPV vaccination history

### Nguồn gốc: Adolescent-specific considerations which — đoạn 496

» Recording data for later use

### Nguồn gốc: Adolescent-specific considerations which — đoạn 497

decisions

### Nguồn gốc: Adolescent-specific considerations which — đoạn 498

» Vaccination history (including HPV)

### Nguồn gốc: Adolescent-specific considerations which — đoạn 499

» Calculation of body mass index (BMI)

### Nguồn gốc: Adolescent-specific considerations which — đoạn 500

» Information icon for more detailed information and guidance

### Nguồn gốc: Adolescent-specific considerations which — đoạn 501

data

### Nguồn gốc: Adolescent-specific considerations which — đoạn 502

» Last menstrual period

### Nguồn gốc: Adolescent-specific considerations which — đoạn 503

workflows

### Nguồn gốc: Data elements to be collected — đoạn 504

» Age

### Nguồn gốc: Data elements to be collected — đoạn 505

personas

### Nguồn gốc: Data elements to be collected — đoạn 506

User scenarios are helpful tools not only to better understand the context in which a digital tool would operate, but also to provide some insights into what key data elements would need to be recorded and accounted for in the database. Additionally, the context in which the tool would be used, illustrated by the user scenarios, provides insight into some functional and non-functional requirements that the system would also need. For example, in the user scenario for an adolescent client (section 3.3), highlighted in yellow are key data elements that need to be recorded and/or calculated.

### Nguồn gốc: Data elements to be collected — đoạn 507

Highlighted in blue are some elements of decision-support logic that can be automated in the system. Highlighted in green are some key functional and non-functional requirements that should be included in the system, and bolded are some adolescent-specific considerations that should be accounted for.

### Nguồn gốc: 3.4 How to interpret user scenarios for functional requirements — đoạn 508

» Laboratory tests conducted » Current medications » Risk factors » Height

### Nguồn gốc: 3.4 How to interpret user scenarios for functional requirements — đoạn 509

indicators

### Nguồn gốc: 3.4 How to interpret user scenarios for functional requirements — đoạn 510

» Weight » Blood pressure » Pulse rate » Gestational age » Services, medication and supplements provided

### Nguồn gốc: 3.4 How to interpret user scenarios for functional requirements — đoạn 511

requirements

### Nguồn gốc: recommendations — đoạn 512

A business process, or simply “process”, is a set of related activities or tasks performed together to achieve the objectives of the health programme area, such as registration, counselling, referrals (14). Workflows are a visual representation of the progression of activities (tasks, interactions, decision points) that are performed within the business process (15). The workflow provides a “story” for the business process being diagrammed and is used to enhance communication and collaboration among users, stakeholders and engineers.

### Nguồn gốc: recommendations — đoạn 513

Table 5 provides an overview of the key business processes conducted by the ANM/midwife persona within ANC service delivery. The workflows for these business processes are detailed in this section. Additionally, for each of these business processes, the data elements and decision-support needs are detailed within components 5 (core data elements) and 6 (decision-support logic) of this DAK.

### Nguồn gốc: recommendations — đoạn 514

Note: The DAK currently provides the data elements and decision-support logic for business processes A (registration) and B (routine ANC contact). The components for the remaining processes will be detailed in future versions. In addition, business processes A, B and C assume ANC to be provided at the facility level and business process D is at the community level. If there are other service-delivery models the country is using based on their context, these can then be modified accordingly.

### Nguồn gốc: recommendations — đoạn 515

requirements

### Nguồn gốc: recommendations — đoạn 516

indicators

### Nguồn gốc: recommendations — đoạn 517

decisions

### Nguồn gốc: recommendations — đoạn 518

data

### Nguồn gốc: recommendations — đoạn 519

workflows

### Nguồn gốc: recommendations — đoạn 520

scenarios

### Nguồn gốc: recommendations — đoạn 521

personas

### Nguồn gốc: Business — đoạn 522

processes and workflows

### Nguồn gốc: recommendations — đoạn 523

Table 5. Overview of key ANC processes

### Nguồn gốc: ID used to — đoạn 524

reference this process throughout the DAK

### Nguồn gốc: Individuals interacting to — đoạn 525

complete the process

### Nguồn gốc: A concrete statement — đoạn 526

describing what the process seeks to achieve

### Nguồn gốc: ANC.A — đoạn 527

» Woman (may or may not yet be confirmed to be pregnant)

### Nguồn gốc: To identify and register a — đoạn 528

pregnant woman so that she can proceed to the

### Nguồn gốc: Starting point: Woman arrives at clinica — đoạn 529

» Data entry clerk or » Auxiliary nurse midwife (ANM) or nurse midwife

### Nguồn gốc: Routine ANC — đoạn 530

contact

### Nguồn gốc: ANC.B — đoạn 531

» Pregnant woman » ANM or nurse midwife

### Nguồn gốc: ANC.B — đoạn 532

» Query pregnant woman’s health record » Register pregnant woman’s details » Update record with registration information, if needed

### Nguồn gốc: ANC services to pregnant — đoạn 533

woman

### Nguồn gốc: ANC services to pregnant — đoạn 534

scenarios

### Nguồn gốc: ANC services to pregnant — đoạn 535

B

### Nguồn gốc: ANC services to pregnant — đoạn 536

» Quick check to ensure the woman does not need urgent medical attention

### Nguồn gốc: ANC services to pregnant — đoạn 537

personas

### Nguồn gốc: ANC services to pregnant — đoạn 538

A

### Nguồn gốc: Process Name — đoạn 539

Starting point: Woman has been registered and is ready to begin ANC contact » Record health history » Assess danger signs

### Nguồn gốc: Process Name — đoạn 540

» Case management or referral » Schedule follow-up contact » Provide counselling on all health topics necessary (e.g. nutrition, HIV testing, family planning, psychosocial and support services)

### Nguồn gốc: ANC.C — đoạn 541

» Pregnant woman » ANM or nurse midwife

### Nguồn gốc: Starting point: Woman requires referral — đoạn 542

» Assess pregnant woman’s danger signs » Stabilize pregnant woman’s condition » Arrange emergency transport

### Nguồn gốc: Starting point: Woman requires referral — đoạn 543

decisions

### Nguồn gốc: Starting point: Woman requires referral — đoạn 544

» Referral health-care provider

### Nguồn gốc: To provide timely and — đoạn 545

appropriate referrals to a higher-level facility or health-care provider

### Nguồn gốc: To provide timely and — đoạn 546

data

### Nguồn gốc: To provide timely and — đoạn 547

C

### Nguồn gốc: To provide timely and — đoạn 548

» Liaise with next level facility for care upon arrival D

### Nguồn gốc: ANC.D — đoạn 549

» Pregnant woman » ANM or nurse midwife » Community health worker

### Nguồn gốc: To provide routine health — đoạn 550

promotion and follow-up within the community

### Nguồn gốc: Starting point: Woman receives community/home-based contact — đoạn 551

» Visit pregnant woman at home or provide outreach in the community » Conduct basic assessment » Determine referral to facility » Distribute commodities (e.g. iron and folic acid, supplements, condoms for protection against STIs)

### Nguồn gốc: Starting point: Woman receives community/home-based contact — đoạn 552

requirements

### Nguồn gốc: Business processes and workflows — đoạn 553

indicators

### Nguồn gốc: ANC health — đoạn 554

promotion, follow-up in the community

### Nguồn gốc: ANC health — đoạn 555

workflows

### Nguồn gốc: ANC health — đoạn 556

» Assess current pregnancy conditions, including symptoms, physical exam and lab tests

### Nguồn gốc: recommendations — đoạn 557

personas

### Nguồn gốc: recommendations — đoạn 558

E

### Nguồn gốc: ID used to — đoạn 559

reference this process throughout the DAK

### Nguồn gốc: Individuals interacting to — đoạn 560

complete the process

### Nguồn gốc: A concrete statement — đoạn 561

describing what the process seeks to achieve

### Nguồn gốc: Reporting on — đoạn 562

aggregate indicators

### Nguồn gốc: ANC.E — đoạn 563

» ANM or nurse midwife

### Nguồn gốc: Compile and submit — đoạn 564

relevant data contributing to indicators and other reporting needs on a routine basis

### Nguồn gốc: Starting point: Time for periodic (usually monthly) reporting — đoạn 565

» ANM/nurse supervisors » Facility manager » District manager

### Nguồn gốc: Starting point: Time for periodic (usually monthly) reporting — đoạn 566

» Tally and compile required data (if on paper) » Check data quality » Correct fixable errors

### Nguồn gốc: Starting point: Time for periodic (usually monthly) reporting — đoạn 567

» Generate and review aggregate reports » Submit to supervisor/director for approval » Provide feedback and any changes required

### Nguồn gốc: Starting point: Time for periodic (usually monthly) reporting — đoạn 568

workflows

### Nguồn gốc: Starting point: Time for periodic (usually monthly) reporting — đoạn 569

Source: Adapted from PATH (13).

### Nguồn gốc: Starting point: Time for periodic (usually monthly) reporting — đoạn 570

data

### Nguồn gốc: Starting point: Time for periodic (usually monthly) reporting — đoạn 571

a As this DAK is focused on ANC, we will refer to this individual as “woman/pregnant woman” although data elements in the registration workflow will use the term “client” for consistency across other DAKs as the registration workflow is a general process across all health service delivery.

### Nguồn gốc: Starting point: Time for periodic (usually monthly) reporting — đoạn 572

The workflows are structured using the standardized notations for business process mapping detailed in Table 6.

### Nguồn gốc: Starting point: Time for periodic (usually monthly) reporting — đoạn 573

decisions

### Nguồn gốc: Starting point: Time for periodic (usually monthly) reporting — đoạn 574

scenarios

### Nguồn gốc: Starting point: Time for periodic (usually monthly) reporting — đoạn 575

» Track loss to follow-up and monitor upcoming contacts for pregnant women in the catchment area

### Nguồn gốc: Pool — đoạn 576

A pool consists of multiple “swim lanes” that depict all the individuals or types of users that are involved in carrying out the business process or workflow. Diagrams should be clear, neat and easy for all viewers to understand the relationship across the different swim lanes. For example, a pool would depict the business process of conducting an outreach activity, which involves multiple stakeholders represented by different lanes in that pool.

### Nguồn gốc: Swim lane — đoạn 577

Each individual or type of user is assigned to a swim lane, a designated area for noting the activities performed or expected by that specific actor. For example, an antenatal care health worker may have one swim lane; the supervisor would be in another swim lane; the clients/patients would be classified in another swim lane.

### Nguồn gốc: Lane 3 — đoạn 578

requirements

### Nguồn gốc: Lane 1 — đoạn 579

indicators

### Nguồn gốc: Lane 1 — đoạn 580

Table 6. Business process symbols used in workflows

### Nguồn gốc: Trigger event — đoạn 581

The workflow diagram should contain both a start and an end event, defining the beginning and completion of the task, respectively.

### Nguồn gốc: End event — đoạn 582

There can be multiple end events depicted across multiple swim lanes in a business process diagram. However, for diagram clarity, there should only be one end event per swim lane.

### Nguồn gốc: Step or Task — đoạn 583

Each activity should start with a verb, e.g. “Register client”, “Calculate risk”. Between the start and end of a task, there should be a series of activities noting the successive actions performed by the actor in that swim lane. There can also be subprocesses within each activity.

### Nguồn gốc: Activity with — đoạn 584

subprocess

### Nguồn gốc: Activity with — đoạn 585

This denotes an activity that has a much longer subprocess to be detailed in another diagram. If the diagram starts to become too complex and unhelpful, the subprocess symbol should be used to reference another process depicted on another page.

### Nguồn gốc: Activity with — đoạn 586

business rule

### Nguồn gốc: Activity with — đoạn 587

This denotes a decision-making activity that requires the business rule, or decision-support logic, to be detailed in a “decision-support table”. This means that the logic described in the decision-support table will come into play during this activity as outlined in the business process. This is usually reserved for complex decisions.

### Nguồn gốc: Sequence flow — đoạn 588

This denotes the flow direction from one process to the next. The end event should not have any output arrows. All symbols (except start event) may have an unlimited number of input arrows. All symbols (except end event and gateway) should have one and only one output arrow. All other symbols should have one output arrow leading to a new symbol, looping back to a previously used symbol or to the end event symbol. Connecting arrows should not intersect (cross) each other.

### Nguồn gốc: Message flow — đoạn 589

This denotes the flow of data or information from one process to another. This is usually used for when data are shared across swim lanes or stakeholder groups.

### Nguồn gốc: Gateway — đoạn 590

This symbol is used to depict a fork, or decision point, in the workflow, which may be a simple binary (e.g. yes/no) filter with two corresponding output arrows, or a different set of outputs.

### Nguồn gốc: Gateway — đoạn 591

personas

### Nguồn gốc: Gateway — đoạn 592

The “Catch – Link” serves as the end of an off-page connector. It is the start of the new process on a different page from the “Throw – Link” or the start of a subprocess that is part of a larger process. There needs to be a “Throw – Link” that is aligned to the “Catch – Link”.

### Nguồn gốc: Ad hoc — đoạn 593

subprocess

### Nguồn gốc: Ad hoc — đoạn 594

An ad hoc subprocess can contain multiple tasks. One or more tasks in this shape should be performed, and they can be performed in any order. However, not all of these activities need to be finished before moving on to the next activity.

### Nguồn gốc: Ad hoc — đoạn 595

~

### Nguồn gốc: Ad hoc — đoạn 596

requirements

### Nguồn gốc: Business processes and workflows — đoạn 597

indicators

### Nguồn gốc: Catch – Link — đoạn 598

decisions

### Nguồn gốc: Catch – Link — đoạn 599

The “Throw – Link” serves as the start of an off-page connector. It is the end of the process when there is no more room on your page for that workflow. It is the end of a process on your current page or the end of a subprocess that is part of a larger process. There will need to be a “Catch – Link” that follows the “Throw – Link”.

### Nguồn gốc: Catch – Link — đoạn 600

data

### Nguồn gốc: Throw – Link — đoạn 601

workflows

### Nguồn gốc: Throw – Link — đoạn 602

There should only be two different outputs that originate from the decision point. If you find yourself needing more than two “output” or sequence flow arrows, you most likely are trying to depict “decision-support logic” or a “business rule”. This should be depicted as an “Activity with business rule” (above) instead.

### Nguồn gốc: Throw – Link — đoạn 603

scenarios

### Nguồn gốc: Throw – Link — đoạn 604

+

### Nguồn gốc: recommendations — đoạn 605

requirements

### Nguồn gốc: management — đoạn 606

indicators

### Nguồn gốc: management — đoạn 607

decisions

### Nguồn gốc: Community — đoạn 608

data

### Nguồn gốc: Community — đoạn 609

workflows

### Nguồn gốc: Community — đoạn 610

scenarios

### Nguồn gốc: Health facility — đoạn 611

personas

### Nguồn gốc: Health facility — đoạn 612

Fig. 5. Overview of key ANC processesa

### Nguồn gốc: Health facility — đoạn 613

Grey boxes are not covered in this DAK. a For key, see Table 6 (page 30).

### Nguồn gốc: A. Business process for registration — đoạn 614

scenarios

### Nguồn gốc: A. Business process for registration — đoạn 615

Objective: To identify and register a pregnant woman so that she can proceed to the ANC consultation.

### Nguồn gốc: Client — đoạn 616

Fig. 6. Workflow: Registration business processª 1.

### Nguồn gốc: Arrive at — đoạn 617

facility

### Nguồn gốc: Health worker or Clerk — đoạn 618

No 4.

### Nguồn gốc: Gather client — đoạn 619

details

### Nguồn gốc: Gather client — đoạn 620

decisions

### Nguồn gốc: Health — đoạn 621

facility

### Nguồn gốc: 3. Urgent — đoạn 622

referral necessary ?

### Nguồn gốc: 3. Urgent — đoạn 623

data

### Nguồn gốc: 2. Rapid — đoạn 624

assessment and

### Nguồn gốc: management — đoạn 625

(RAM)

### Nguồn gốc: management — đoạn 626

workflows

### Nguồn gốc: 8. Validate client details — đoạn 627

6.

### Nguồn gốc: Match — đoạn 628

found?

### Nguồn gốc: Yes — đoạn 629

No

### Nguồn gốc: Yes — đoạn 630

8.1

### Nguồn gốc: Review — đoạn 631

sociodemographic data with client

### Nguồn gốc: Review — đoạn 632

8.2

### Nguồn gốc: Update — đoạn 633

needed?

### Nguồn gốc: Update — đoạn 634

8.3

### Nguồn gốc: Yes Update client — đoạn 635

details No

### Nguồn gốc: Yes Update client — đoạn 636

9.

### Nguồn gốc: Check-in — đoạn 637

client

### Nguồn gốc: B. ANC — đoạn 638

contact

### Nguồn gốc: B. ANC — đoạn 639

indicators

### Nguồn gốc: B. ANC — đoạn 640

5.

### Nguồn gốc: Search for — đoạn 641

client

### Nguồn gốc: Search for — đoạn 642

7.

### Nguồn gốc: Create client — đoạn 643

record

### Nguồn gốc: Create client — đoạn 644

requirements

### Nguồn gốc: Create client — đoạn 645

a For key, see Table 6 (page 30). Source: Adapted from PATH (13).

### Nguồn gốc: Business processes and workflows — đoạn 646

personas

### Nguồn gốc: Business processes and workflows — đoạn 647

The workflows that follow depict processes that have not been generalized across different contexts and may not reflect the variability and nuances across different settings. The simplicity of the workflow may not adequately illustrate the non-linear steps that may occur. These workflows are considered to be workflows to be “80% complete,” whereby the “other 20 %” will need to be done through a series of human-centred design methods and mechanisms to complete the workflows for an implementation.

### Nguồn gốc: recommendations — đoạn 648

personas scenarios workflows data

### Nguồn gốc: General note — đoạn 649

Registration may be conducted as a stand-alone process by a data clerk/ administrative persona ahead of the ANC encounter with a clinical healthcare provider or it may be conducted directly by the health-care provider as part of the overall ANC encounter.

### Nguồn gốc: 1. Arrive at facility — đoạn 650

» Woman arrives at the facility. »

### Nguồn gốc: 1. Arrive at facility — đoạn 651

She may or may not have an identification card with her.

### Nguồn gốc: 1. Arrive at facility — đoạn 652

»

### Nguồn gốc: 1. Arrive at facility — đoạn 653

Client could already be registered at the health-care facility for another service.

### Nguồn gốc: 2. Rapid assessment and management (RAM) — đoạn 654

» The registration clerk or the first health worker the woman encounters assesses for any possible visible danger signs. »

### Nguồn gốc: 2. Rapid assessment and management (RAM) — đoạn 655

Note: there could be decision-support logic input here as well, but for the reference content we have determined that RAM is outside of the scope of this ANC DAK.

### Nguồn gốc: 3. Urgent referral necessary? — đoạn 656

» Determine whether urgent referral is required based on the RAM. »

### Nguồn gốc: 3. Urgent referral necessary? — đoạn 657

If yes, initiate referral (see Business process C: ANC referral).

### Nguồn gốc: 4. Gather client details — đoạn 658

» The health worker or data clerk searches for the woman’s name using available identifiers. »

### Nguồn gốc: 4. Gather client details — đoạn 659

Ask the client whether they have previously been issued a unique identifier.

### Nguồn gốc: 4. Gather client details — đoạn 660

»

### Nguồn gốc: Does the client have an identification card/number/barcode? — đoạn 661

»

### Nguồn gốc: Does the client have an identification card/number/barcode? — đoạn 662

Does client say whether she is a returning or a referred client?

### Nguồn gốc: Does the client have an identification card/number/barcode? — đoạn 663

»

### Nguồn gốc: Does the client have an identification card/number/barcode? — đoạn 664

If a referral, check for referral slip or data from the community.

### Nguồn gốc: Does the client have an identification card/number/barcode? — đoạn 665

»

### Nguồn gốc: Determine whether the client is new to the health-care facility/health — đoạn 666

post.

### Nguồn gốc: Determine whether the client is new to the health-care facility/health — đoạn 667

»

### Nguồn gốc: Determine whether the client is new to the health-care facility/health — đoạn 668

For returning clients, details will be retrieved from the registry of clients at this facility or, if possible, from a central client registry.

### Nguồn gốc: 5. Search for client — đoạn 669

» This search process can be done through a variety of means depending on what mechanisms are available in country. For example, clients can be searched for by using their name, unique identifier, a QR code or even biometrics.

### Nguồn gốc: 6. Match found? — đoạn 670

» If multiple records are found and no unique ID, provide option to merge records.

### Nguồn gốc: 7. Create client record — đoạn 671

» Issue a unique identifier, if used and possible at the facility.

### Nguồn gốc: 7. Create client record — đoạn 672

requirements

### Nguồn gốc: 7. Create client record — đoạn 673

indicators

### Nguồn gốc: 7. Create client record — đoạn 674

decisions

### Nguồn gốc: 8. Validate client details — đoạn 675

» Review and update client record.

### Nguồn gốc: 8.1. Review sociodemographic data with client — đoạn 676

Review client’s non-clinical information – name, address, contact information, etc.

### Nguồn gốc: 8.1. Review sociodemographic data with client — đoạn 677

personas

### Nguồn gốc: 8.2. Update needed? — đoạn 678

Has the client moved? Has she changed her contact information or has any other sociodemographic information changed?

### Nguồn gốc: 8.3. Update client details — đoạn 679

Client can provide updated information if she has recently moved or changed other details. Merge/update client records.

### Nguồn gốc: 8.3. Update client details — đoạn 680

»

### Nguồn gốc: 8.3. Update client details — đoạn 681

May also happen during counselling process.

### Nguồn gốc: 8.3. Update client details — đoạn 682

»

### Nguồn gốc: 8.3. Update client details — đoạn 683

In some contexts, this may be linked to billing needs.

### Nguồn gốc: 8.3. Update client details — đoạn 684

scenarios

### Nguồn gốc: 8.3. Update client details — đoạn 685

»

### Nguồn gốc: 8.3. Update client details — đoạn 686

workflows

### Nguồn gốc: 9. Check in client — đoạn 687

» The pregnant woman waits in line to be called by the health worker for the ANC contact.

### Nguồn gốc: 9. Check in client — đoạn 688

data decisions indicators

### Nguồn gốc: 9. Check in client — đoạn 689

requirements

### Nguồn gốc: Business process for ANC contact — đoạn 690

Objective: To counsel and provide routine ANC services to pregnant women. Fig. 7. Workflow: ANC contacta

### Nguồn gốc: Business process for ANC contact — đoạn 691

3.

### Nguồn gốc: Confirm — đoạn 692

pregnancy

### Nguồn gốc: Confirm — đoạn 693

No

### Nguồn gốc: Health — đoạn 694

facility

### Nguồn gốc: Health — đoạn 695

workflows

### Nguồn gốc: Health — đoạn 696

scenarios

### Nguồn gốc: Start — đoạn 697

decisions

### Nguồn gốc: Start — đoạn 698

data

### Nguồn gốc: Arrive at — đoạn 699

facility

### Nguồn gốc: Client — đoạn 700

personas

### Nguồn gốc: recommendations — đoạn 701

B.

### Nguồn gốc: recommendations — đoạn 702

A.

### Nguồn gốc: Assessment — đoạn 703

and

### Nguồn gốc: Management — đoạn 704

(RAM)

### Nguồn gốc: 2. Danger — đoạn 705

signs needing referral?

### Nguồn gốc: Pregnant — đoạn 706

4.

### Nguồn gốc: First — đoạn 707

contact?

### Nguồn gốc: 5. Quick — đoạn 708

check

### Nguồn gốc: Yes — đoạn 709

indicators

### Nguồn gốc: Yes — đoạn 710

10.1 co

### Nguồn gốc: Not first contact — đoạn 711

requirements

### Nguồn gốc: Not first contact — đoạn 712

10.

### Nguồn gốc: Not first contact — đoạn 713

6.

### Nguồn gốc: Collect — đoạn 714

woman’s profile & history

### Nguồn gốc: 7. Check — đoạn 715

symptoms & follow-up

### Nguồn gốc: 7. Check — đoạn 716

8.

### Nguồn gốc: Conduct — đoạn 717

physical exam

### Nguồn gốc: 9. Conduct — đoạn 718

laboratory tests and imaging

### Nguồn gốc: 9. Conduct — đoạn 719

10.4 &t

### Nguồn gốc: recommendations — đoạn 720

personas

### Nguồn gốc: Yes — đoạn 721

14.

### Nguồn gốc: Woman — đoạn 722

returns?

### Nguồn gốc: Woman — đoạn 723

No

### Nguồn gốc: End — đoạn 724

scenarios

### Nguồn gốc: End — đoạn 725

13.

### Nguồn gốc: Self-care — đoạn 726

interventions in the home or community

### Nguồn gốc: Self-care — đoạn 727

workflows

### Nguồn gốc: Self-care — đoạn 728

10.2

### Nguồn gốc: Physiological — đoạn 729

symptoms counselling

### Nguồn gốc: 10.3 Diet — đoạn 730

counselling

### Nguồn gốc: 10.4 Diagnosis — đoạn 731

& treatment

### Nguồn gốc: 10.5 Nutrition — đoạn 732

supplementation

### Nguồn gốc: 10.6 Risk — đoạn 733

reduction & general counselling

### Nguồn gốc: 10.6 Risk — đoạn 734

11.

### Nguồn gốc: Referral — đoạn 735

needed?

### Nguồn gốc: C. ANC referral — đoạn 736

indicators

### Nguồn gốc: C. ANC referral — đoạn 737

10.7

### Nguồn gốc: 10.8 Intimate — đoạn 738

partner violence first-line treatment and care

### Nguồn gốc: 10.8 Intimate — đoạn 739

No

### Nguồn gốc: 10.8 Intimate — đoạn 740

decisions

### Nguồn gốc: 10.1 Behaviour — đoạn 741

counselling

### Nguồn gốc: 10.1 Behaviour — đoạn 742

12.

### Nguồn gốc: Scheduling — đoạn 743

data

### Nguồn gốc: 10.9 Deworming — đoạn 744

& malaria counselling & prophylaxis

### Nguồn gốc: 10.9 Deworming — đoạn 745

requirements

### Nguồn gốc: Business processes and workflows — đoạn 746

SRH: sexual and reproductive health. a For key, see Table 6 (page 30). Source: Adapted from PATH (13).

### Nguồn gốc: General note — đoạn 747

» Woman arrives at the facility is shown here to depict the completeness of an ANC contact. »

### Nguồn gốc: General note — đoạn 748

Woman arrives at health-care facility to be seen by a health worker.

### Nguồn gốc: General note — đoạn 749

»

### Nguồn gốc: The registration process is to record administrative and demographic — đoạn 750

information as detailed in Business process A: registration.

### Nguồn gốc: The registration process is to record administrative and demographic — đoạn 751

»

### Nguồn gốc: The registration process is to record administrative and demographic — đoạn 752

The general registration step may not need to be done at each contact if ANC services are being provided in a community setting or the woman is being followed up by the same health worker.

### Nguồn gốc: The registration process is to record administrative and demographic — đoạn 753

scenarios

### Nguồn gốc: The registration process is to record administrative and demographic — đoạn 754

personas

### Nguồn gốc: ANC CONTACT BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 755

Reminder for all health workers:

### Nguồn gốc: ANC CONTACT BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 756

requirements

### Nguồn gốc: ANC CONTACT BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 757

indicators

### Nguồn gốc: ANC CONTACT BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 758

decisions

### Nguồn gốc: ANC CONTACT BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 759

data

### Nguồn gốc: ANC CONTACT BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 760

workflows

### Nguồn gốc: Follow respectful maternity care principles when delivering ANC care, — đoạn 761

treating all pregnant women (regardless of background or health status) respectfully and non-judgmentally. For further training on respectful maternity care and effective provider-client communication, see www.qualityofcarenetwork.org/knowledge-library.

### Nguồn gốc: 1. Rapid assessment and management (RAM) — đoạn 762

» The registration clerk or the first health worker the woman encounters assesses for any visible danger signs.

### Nguồn gốc: 2. Danger signs needing referral? — đoạn 763

» Health worker determines whether the woman presents symptoms or health concern requiring an urgent referral based on RAM. »

### Nguồn gốc: 2. Danger signs needing referral? — đoạn 764

If referral is needed, health worker refers immediately based on danger signs presented and proceeds to Business process C: ANC referral.

### Nguồn gốc: 3. Confirm pregnancy — đoạn 765

» Pregnancy can be confirmed using a urine dipstick test or by a blood test. »

### Nguồn gốc: 3. Confirm pregnancy — đoạn 766

This task is optional later in pregnancy.

### Nguồn gốc: 4. First contact — đoạn 767

» Health worker determines whether this is the woman’s first ANC contact or if she is returning for a follow-up.

### Nguồn gốc: 5. Quick check — đoạn 768

» Health worker assesses woman to see whether there are any danger signs that warrant in-facility management or referral, or if possible, to proceed with the routine ANC contact.

### Nguồn gốc: 6. Collect woman’s profile and history — đoạn 769

» If this is the woman’s first ANC contact, the health worker records information on her current pregnancy, past medical and obstetric history, medications, behaviour, immunization status and other

### Nguồn gốc: 7. Check symptoms and follow up — đoạn 770

» Health worker follows up on any previously reported behaviours, medications and symptoms. Steps 8–10 below are all conducted within the ANC assessment.

### Nguồn gốc: 8. Conduct physical exam — đoạn 771

» Health worker conducts a physical exam for weight, height (if first contact), maternal exam and fetal assessment.

### Nguồn gốc: 9. Conduct laboratory test and imaging — đoạn 772

» Health worker orders or follows up on required laboratory tests and ultrasound, as necessary. »

### Nguồn gốc: 9. Conduct laboratory test and imaging — đoạn 773

Health worker informs woman of all tests ordered and explains why.

### Nguồn gốc: 10. Counselling, in-facility management and treatment — đoạn 774

» Based on the previous steps, health worker provides counselling for potential risk, behaviours and diagnoses, as well as preventive services and any treatment that can be provided at the facility, including through admission to a different part of the facility.

### Nguồn gốc: 11. Referral needed? — đoạn 775

» If there are any diagnoses requiring referral or services that cannot be provided at the facility, health worker refers pregnant woman based on

### Nguồn gốc: findings during counselling. — đoạn 776

»

### Nguồn gốc: findings during counselling. — đoạn 777

Provide counselling and treatment for: hospital referral;

### Nguồn gốc: findings during counselling. — đoạn 778

–

### Nguồn gốc: findings during counselling. — đoạn 779

behaviour counselling (caffeine reduction, tobacco cessation, second-hand smoke reduction, condom use, alcohol/substance use);

### Nguồn gốc: 12. Scheduling — đoạn 780

» If a referral is not required, health worker schedules follow-up contact.

### Nguồn gốc: 12. Scheduling — đoạn 781

–

### Nguồn gốc: 12. Scheduling — đoạn 782

diet and exercise (healthy eating, protein intake);

### Nguồn gốc: 12. Scheduling — đoạn 783

–

### Nguồn gốc: 12. Scheduling — đoạn 784

diagnostics (hypertension, HIV, hepatitis B, hepatitis C, syphilis, asymptomatic bacteriuria [ASB], diabetes, anaemia, tuberculosis, diabetes in pregnancy, Rh factor testing, blood typing);

### Nguồn gốc: 12. Scheduling — đoạn 785

–

### Nguồn gốc: 12. Scheduling — đoạn 786

risk (pre-eclampsia risk, HIV risk, diabetes risk, danger signs);

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 787

» Woman monitors her pregnancy and general condition until the next interaction with the health-care provider (follow-up contact, delivery or symptom/health concern). She also self-manages physiological symptoms and health behaviours, including diet and exercise, following the options promoted by the provider during counselling (step 10).

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 788

–

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 789

ANC contact schedule;

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 790

»

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 791

–

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 792

birth plan;

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 793

–

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 794

postpartum family planning;

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 795

–

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 796

breastfeeding;

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 797

–

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 798

prevention (anaemia, deworming, malaria prophylaxis, immunizations [TTCV, flu]);

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 799

–

### Nguồn gốc: 13. Self-care interventions in the home or community — đoạn 800

vitamin supplementation (iron and folic acid, calcium, vitamin A)

### Nguồn gốc: 14. Woman returns — đoạn 801

» Woman either comes back for follow-up services or is referred until delivery.

### Nguồn gốc: 14. Woman returns — đoạn 802

decisions

### Nguồn gốc: 14. Woman returns — đoạn 803

*

### Nguồn gốc: If danger signs arise or physiological symptoms of pregnancy persist — đoạn 804

even after the woman tries the self-management options, the woman should contact a health worker and/or facility.

### Nguồn gốc: If danger signs arise or physiological symptoms of pregnancy persist — đoạn 805

data

### Nguồn gốc: If danger signs arise or physiological symptoms of pregnancy persist — đoạn 806

physiological symptoms (nausea and vomiting relief, heartburn, constipation, lower back or pelvic pain, varicose veins, oedema);

### Nguồn gốc: If danger signs arise or physiological symptoms of pregnancy persist — đoạn 807

workflows

### Nguồn gốc: If danger signs arise or physiological symptoms of pregnancy persist — đoạn 808

–

### Nguồn gốc: If danger signs arise or physiological symptoms of pregnancy persist — đoạn 809

scenarios

### Nguồn gốc: If danger signs arise or physiological symptoms of pregnancy persist — đoạn 810

–

### Nguồn gốc: These services could also include psychosocial, emotional, cultural and — đoạn 811

social support services to help the pregnant woman. Examples of these services are psychosocial counselling, peer-support organizations, and social service organizations for financial assistance, nutrition assistance and education, and childcare.

### Nguồn gốc: These services could also include psychosocial, emotional, cultural and — đoạn 812

personas

### Nguồn gốc: These services could also include psychosocial, emotional, cultural and — đoạn 813

»

### Nguồn gốc: management, healthy behaviours, and making a birth plan, provide — đoạn 814

the woman with options where possible to give her the knowledge and ability to make decisions about her and her baby’s health. Also, provide clear directions on when to contact health worker or facility if danger signs arise or self-management of symptoms is not satisfactory.

### Nguồn gốc: recommendations — đoạn 815

»

### Nguồn gốc: recommendations — đoạn 816

dietary supplements not recommended include: high protein supplement

### Nguồn gốc: recommendations — đoạn 817

•

### Nguồn gốc: recommendations — đoạn 818

vitamin B6 supplement

### Nguồn gốc: recommendations — đoạn 819

•

### Nguồn gốc: recommendations — đoạn 820

vitamins C and E supplement

### Nguồn gốc: recommendations — đoạn 821

•

### Nguồn gốc: recommendations — đoạn 822

vitamin D supplement.

### Nguồn gốc: recommendations — đoạn 823

requirements

### Nguồn gốc: Business processes and workflows — đoạn 824

indicators

### Nguồn gốc: Business processes and workflows — đoạn 825

•

### Nguồn gốc: Business process for ANC referral — đoạn 826

Objective: To provide timely and appropriate referrals to a higher-level facility or health-care provider. Fig. 8. Workflow: ANC referrala 3.

### Nguồn gốc: Identify & — đoạn 827

discuss referral location options

### Nguồn gốc: Woman — đoạn 828

personas

### Nguồn gốc: Client goes to — đoạn 829

referral facility

### Nguồn gốc: Client goes to — đoạn 830

4.

### Nguồn gốc: Contact — đoạn 831

referral facility

### Nguồn gốc: Contact — đoạn 832

1.

### Nguồn gốc: Emergency — đoạn 833

referral?

### Nguồn gốc: Emergency — đoạn 834

data

### Nguồn gốc: Emergency — đoạn 835

workflows

### Nguồn gốc: Health — đoạn 836

facility

### Nguồn gốc: Health — đoạn 837

scenarios

### Nguồn gốc: Health — đoạn 838

No

### Nguồn gốc: recommendations — đoạn 839

C.

### Nguồn gốc: recommendations — đoạn 840

6.

### Nguồn gốc: Provide — đoạn 841

information to referral facility

### Nguồn gốc: Yes — đoạn 842

2.1

### Nguồn gốc: Stabilize — đoạn 843

client & give pre-referral treatment

### Nguồn gốc: Stabilize — đoạn 844

2.2

### Nguồn gốc: Woman — đoạn 845

stable enough to transport?

### Nguồn gốc: Yes — đoạn 846

2.3

### Nguồn gốc: Organize — đoạn 847

transport

### Nguồn gốc: Organize — đoạn 848

No

### Nguồn gốc: Yes — đoạn 849

requirements

### Nguồn gốc: Nurse/Midwife — đoạn 850

indicators

### Nguồn gốc: Referral health — đoạn 851

health facility

### Nguồn gốc: Referral — đoạn 852

facility

### Nguồn gốc: Referral — đoạn 853

decisions

### Nguồn gốc: Referral — đoạn 854

No

### Nguồn gốc: Referral — đoạn 855

5.

### Nguồn gốc: Can facility — đoạn 856

accommodate?

### Nguồn gốc: Can facility — đoạn 857

For key, see Table 6 (page 30). Source: PATH (13). a

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 858

7.

### Nguồn gốc: Discuss any — đoạn 859

questions with client

### Nguồn gốc: Discuss any — đoạn 860

E

### Nguồn gốc: recommendations — đoạn 861

personas

### Nguồn gốc: recommendations — đoạn 862

goes to erral cility

### Nguồn gốc: recommendations — đoạn 863

scenarios

### Nguồn gốc: End — đoạn 864

workflows

### Nguồn gốc: End — đoạn 865

7. uss any stions client

### Nguồn gốc: End — đoạn 866

data

### Nguồn gốc: End — đoạn 867

9.

### Nguồn gốc: Emergency — đoạn 868

obstetric care

### Nguồn gốc: Emergency — đoạn 869

decisions

### Nguồn gốc: Emergency — đoạn 870

8.

### Nguồn gốc: Receives — đoạn 871

woman

### Nguồn gốc: End — đoạn 872

indicators

### Nguồn gốc: End — đoạn 873

requirements

### Nguồn gốc: Business processes and workflows — đoạn 874

» personas

### Nguồn gốc: General notes — đoạn 875

Examples of reasons why a referral may be needed include: the health worker cannot perform the “service” (e.g. facility is not equipped for specific service);

### Nguồn gốc: General notes — đoạn 876

»

### Nguồn gốc: General notes — đoạn 877

there is an emergency and the client needs immediate referral;

### Nguồn gốc: General notes — đoạn 878

»

### Nguồn gốc: General notes — đoạn 879

the woman presents risk factors that cannot be managed at the healthcare facility.

### Nguồn gốc: General notes — đoạn 880

scenarios

### Nguồn gốc: 1. Emergency referral? — đoạn 881

» If woman needs an immediate referral due to an emergency situation, bypass standard referral steps. In the case of an emergency, a referral can be made at any time including during registration, counselling and service provision.

### Nguồn gốc: 2.1 Stabilize woman and give prereferral treatment. — đoạn 882

The woman is assumed to need emergency referral if her condition requires immediate medical attention.

### Nguồn gốc: Thus, stabilize the woman’s condition and provide any necessary — đoạn 883

treatment.

### Nguồn gốc: Thus, stabilize the woman’s condition and provide any necessary — đoạn 884

If the woman is still not stable, provide prereferral treatment for stabilization.

### Nguồn gốc: Organize emergency transport for the woman. For emergency — đoạn 885

referrals, the health-care facility arranges transport usually by phoning for an ambulance or other vehicle.

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 886

» In discussion with the woman and her relatives, decide on where she will be referred. Discussions include:

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 887

»

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 888

–

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 889

how to get to the referral facility, including location and transportation options;

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 890

–

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 891

who to see and what is likely to happen;

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 892

–

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 893

follow-up on return, if needed.

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 894

If there are various possible referral facilities, either the woman or her relatives should indicate their preferred referral location.

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 895

requirements

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 896

indicators

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 897

decisions

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 898

data

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 899

workflows

### Nguồn gốc: 3. Identify and discuss referral location options — đoạn 900

»

### Nguồn gốc: Once the woman is stable enough to transport, then immediately — đoạn 901

transport.

### Nguồn gốc: 7. Discuss any questions with client — đoạn 902

» Discuss any questions or concerns the woman may have.

### Nguồn gốc: 7. Discuss any questions with client — đoạn 903

»

### Nguồn gốc: 7. Discuss any questions with client — đoạn 904

Otherwise, find a different facility that can accommodate the woman.

### Nguồn gốc: 7. Discuss any questions with client — đoạn 905

»

### Nguồn gốc: 7. Discuss any questions with client — đoạn 906

A system can be set up to validate that a facility has both the supplies and skills to accommodate the woman.

### Nguồn gốc: 7. Discuss any questions with client — đoạn 907

»

### Nguồn gốc: For emergency referrals, the health-care facility arranges transport – — đoạn 908

usually by phoning for an ambulance or other vehicle.

### Nguồn gốc: For emergency referrals, the health-care facility arranges transport – — đoạn 909

»

### Nguồn gốc: For emergency referrals, the health-care facility arranges transport – — đoạn 910

Fill out referral form, which can include notification of the referral destination.

### Nguồn gốc: For emergency referrals, the health-care facility arranges transport – — đoạn 911

»

### Nguồn gốc: Provide the necessary clinical, sociodemographic and identity — đoạn 912

information to the referral facility.

### Nguồn gốc: Provide the necessary clinical, sociodemographic and identity — đoạn 913

»

### Nguồn gốc: Provide the necessary clinical, sociodemographic and identity — đoạn 914

This can be done digitally if the appropriate systems are in place.

### Nguồn gốc: Provide the necessary clinical, sociodemographic and identity — đoạn 915

Once the referral facility receives the woman, move to provide services at the receiving referral facility (Business process B: ANC contact).

### Nguồn gốc: 9. Emergency obstetric care — đoạn 916

» Provide necessary care.

### Nguồn gốc: 9. Emergency obstetric care — đoạn 917

decisions

### Nguồn gốc: 9. Emergency obstetric care — đoạn 918

Woman or family arranges own transport.

### Nguồn gốc: 9. Emergency obstetric care — đoạn 919

»

### Nguồn gốc: 9. Emergency obstetric care — đoạn 920

data

### Nguồn gốc: 9. Emergency obstetric care — đoạn 921

»

### Nguồn gốc: 8. Receipt of client — đoạn 922

» Referral facility receives woman along with all the necessary clinical, sociodemographic and identification information.

### Nguồn gốc: 8. Receipt of client — đoạn 923

workflows

### Nguồn gốc: 6. Provide information to referral facility — đoạn 924

» Make appointment, if necessary.

### Nguồn gốc: If an unaccompanied adolescent woman needs a referral, ensure that — đoạn 925

a responsible adult is sent as an accompanying person.

### Nguồn gốc: If an unaccompanied adolescent woman needs a referral, ensure that — đoạn 926

scenarios

### Nguồn gốc: If an unaccompanied adolescent woman needs a referral, ensure that — đoạn 927

If the referral facility can accommodate the woman, move on to step 6: “Provide information to referral facility”.

### Nguồn gốc: If an unaccompanied adolescent woman needs a referral, ensure that — đoạn 928

»

### Nguồn gốc: If an unaccompanied adolescent woman needs a referral, ensure that — đoạn 929

personas

### Nguồn gốc: If an unaccompanied adolescent woman needs a referral, ensure that — đoạn 930

For adolescents:

### Nguồn gốc: 5. Can referral facility accommodate? — đoạn 931

» Check whether the referral facility can accommodate the woman and provide the services she needs. »

### Nguồn gốc: 4. Contact referral facility — đoạn 932

» Health workers should contact the referral facility to determine whether that facility can accommodate such a referral and to ensure the woman is treated as soon as possible upon arrival.

### Nguồn gốc: 4. Contact referral facility — đoạn 933

indicators

### Nguồn gốc: 4. Contact referral facility — đoạn 934

requirements

### Nguồn gốc: Business processes and workflows — đoạn 935

Objective: To provide routine health promotion and follow-up within the community. Fig. 9. Workflow: ANC health promotion follow-up in the communitya

### Nguồn gốc: 8. Self-care — đoạn 936

interventions in the home or community

### Nguồn gốc: 8. Self-care — đoạn 937

scenarios

### Nguồn gốc: Woman — đoạn 938

personas

### Nguồn gốc: recommendations — đoạn 939

D. Business process for ANC health promotion follow-up in the community

### Nguồn gốc: 1. Plans for — đoạn 940

routine, targeted community support

### Nguồn gốc: 1. Plans for — đoạn 941

2.

### Nguồn gốc: Visits woman — đoạn 942

3.

### Nguồn gốc: 7. Schedule — đoạn 943

next visit

### Nguồn gốc: 4. Danger — đoạn 944

signs needing referral?

### Nguồn gốc: End — đoạn 945

requirements

### Nguồn gốc: End — đoạn 946

indicators

### Nguồn gốc: End — đoạn 947

decisions

### Nguồn gốc: End — đoạn 948

data

### Nguồn gốc: End — đoạn 949

workflows

### Nguồn gốc: Community — đoạn 950

No

### Nguồn gốc: management — đoạn 951

in the community

### Nguồn gốc: management — đoạn 952

For key, see Table 6 (page 30). Source: PATH (13). a

### Nguồn gốc: 6. ANC case management in the community — đoạn 953

» Provide counselling on the topics below, enquiring about which strategies the woman has used to manage symptoms and which healthy behaviours she has adopted or plans to use. Then, discuss additional options or reinforcing behaviours, answer any questions and encourage the woman on continuing to make informed decisions for:

### Nguồn gốc: 6. ANC case management in the community — đoạn 954

»

### Nguồn gốc: 6. ANC case management in the community — đoạn 955

Determine whether she needs additional support.

### Nguồn gốc: 6. ANC case management in the community — đoạn 956

physiological symptoms (nausea and vomiting relief, heartburn, supplements, constipation, lower back or pelvic pain, varicose veins, oedema)

### Nguồn gốc: 6. ANC case management in the community — đoạn 957

–

### Nguồn gốc: 6. ANC case management in the community — đoạn 958

diet and exercise (healthy eating, protein intake);

### Nguồn gốc: 6. ANC case management in the community — đoạn 959

–

### Nguồn gốc: 6. ANC case management in the community — đoạn 960

ANC contact schedule;

### Nguồn gốc: 6. ANC case management in the community — đoạn 961

–

### Nguồn gốc: 6. ANC case management in the community — đoạn 962

birth plan;

### Nguồn gốc: 6. ANC case management in the community — đoạn 963

–

### Nguồn gốc: 6. ANC case management in the community — đoạn 964

postpartum family planning;

### Nguồn gốc: 6. ANC case management in the community — đoạn 965

–

### Nguồn gốc: 6. ANC case management in the community — đoạn 966

breastfeeding.

### Nguồn gốc: 6. ANC case management in the community — đoạn 967

Where allowed, provide treatment for: –

### Nguồn gốc: 6. ANC case management in the community — đoạn 968

prevention (anaemia, deworming, malaria prophylaxis, immunizations [TTCV, flu])

### Nguồn gốc: 6. ANC case management in the community — đoạn 969

–

### Nguồn gốc: 6. ANC case management in the community — đoạn 970

Vitamin supplementation (iron and folic acid, calcium, vitamin A).

### Nguồn gốc: 7. Schedule next contact — đoạn 971

» If a referral is not required, CHW schedules follow-up contact/ appointment, at either community or facility level.

### Nguồn gốc: 7. Schedule next contact — đoạn 972

requirements

### Nguồn gốc: Business processes and workflows — đoạn 973

–

### Nguồn gốc: Business processes and workflows — đoạn 974

indicators

### Nguồn gốc: 5. Referral — đoạn 975

» Conduct the referral if needed. Also, provide additional referrals to services that help the pregnant woman. Examples of these services are psychosocial counselling, peer-support organizations, and social service organizations for financial assistance, nutrition assistance and education, and childcare.

### Nguồn gốc: 5. Referral — đoạn 976

behaviour counselling (caffeine reduction, tobacco cessation, second-hand smoke reduction, condom use, alcohol/substance use);

### Nguồn gốc: 5. Referral — đoạn 977

decisions

### Nguồn gốc: 4. Danger signs needing referral? — đoạn 978

» CHW determines whether the woman presents symptoms or concerns requiring an urgent referral.

### Nguồn gốc: 4. Danger signs needing referral? — đoạn 979

»

### Nguồn gốc: 4. Danger signs needing referral? — đoạn 980

–

### Nguồn gốc: 4. Danger signs needing referral? — đoạn 981

data

### Nguồn gốc: 3. Quick check — đoạn 982

» Assess for any possible danger signs, and the woman’s informal (e.g. family, friends) and formal (e.g. community women’s groups, nutritional or financial assistance, childcare) support systems.

### Nguồn gốc: 3. Quick check — đoạn 983

referral to nearest facility, if necessary;

### Nguồn gốc: 3. Quick check — đoạn 984

workflows

### Nguồn gốc: 2. Visits woman or sees her at community day — đoạn 985

» The CHW visits the woman during community day and/or at her home (depending on the context).

### Nguồn gốc: 2. Visits woman or sees her at community day — đoạn 986

–

### Nguồn gốc: 2. Visits woman or sees her at community day — đoạn 987

scenarios

### Nguồn gốc: 1. Plans for routine, targeted community support — đoạn 988

» CHW plans outreach activities, including equipment and supplies (e.g. iron and folic acid supplementation, vaccines, condoms) and any information, education and communication materials.

### Nguồn gốc: 1. Plans for routine, targeted community support — đoạn 989

personas

### Nguồn gốc: General notes — đoạn 990

» Community health workers (CHWs) regularly provide outreach services to the community. » These same workers are linked to and supervised by facility staff, to ensure continuity of care and referral. » CHW tasks and responsibilities will vary from country to country depending on national and subnational ANC policies.

### Nguồn gốc: ANC HEALTH PROMOTION FOLLOW-UP IN THE COMMUNITY BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 991

»

### Nguồn gốc: ANC HEALTH PROMOTION FOLLOW-UP IN THE COMMUNITY BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 992

Woman monitors her pregnancy and general condition until the next interaction with the CHW or the health-care provider (follow-up contact, childbirth or symptom/health concern). She self-manages physiological symptoms and health behaviours, including diet and exercise, following the options promoted by the health workers during counselling (step 6). If physiological symptoms of pregnancy persist even after the woman tries the self-management options, the woman should contact the CHW and/or facility.

### Nguồn gốc: ANC HEALTH PROMOTION FOLLOW-UP IN THE COMMUNITY BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 993

requirements

### Nguồn gốc: ANC HEALTH PROMOTION FOLLOW-UP IN THE COMMUNITY BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 994

indicators

### Nguồn gốc: ANC HEALTH PROMOTION FOLLOW-UP IN THE COMMUNITY BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 995

decisions

### Nguồn gốc: ANC HEALTH PROMOTION FOLLOW-UP IN THE COMMUNITY BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 996

data

### Nguồn gốc: ANC HEALTH PROMOTION FOLLOW-UP IN THE COMMUNITY BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 997

workflows

### Nguồn gốc: ANC HEALTH PROMOTION FOLLOW-UP IN THE COMMUNITY BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 998

scenarios

### Nguồn gốc: ANC HEALTH PROMOTION FOLLOW-UP IN THE COMMUNITY BUSINESS PROCESS NOTES AND ANNOTATIONS — đoạn 999

personas

### Nguồn gốc: 8. Self-care interventions in the home or community — đoạn 1000

» Reminds woman of danger signs and shares emergency transport information, if available.

### Nguồn gốc: recommendations — đoạn 1001

E.

### Nguồn gốc: Business process for reporting on aggregate indicators — đoạn 1002

Objective: Compile and submit relevant data contributing to indicators and other reporting needs on a routine basis.

### Nguồn gốc: Start — đoạn 1003

1.

### Nguồn gốc: Check data — đoạn 1004

quality

### Nguồn gốc: Check data — đoạn 1005

2.

### Nguồn gốc: Correct — đoạn 1006

fixable data quality issues

### Nguồn gốc: Correct — đoạn 1007

3.

### Nguồn gốc: Generate — đoạn 1008

aggregate reports

### Nguồn gốc: Generate — đoạn 1009

4.

### Nguồn gốc: Check — đoạn 1010

aggregate reports

### Nguồn gốc: Check — đoạn 1011

5.

### Nguồn gốc: In-charge — đoạn 1012

review needed?

### Nguồn gốc: In-charge — đoạn 1013

personas

### Nguồn gốc: Facility staff — đoạn 1014

Fig. 10. Reporting on aggregate indicatorsa

### Nguồn gốc: Facility staff — đoạn 1015

No

### Nguồn gốc: Facility staff — đoạn 1016

scenarios

### Nguồn gốc: Facility in-charge — đoạn 1017

6.

### Nguồn gốc: Review report — đoạn 1018

7.

### Nguồn gốc: Changes — đoạn 1019

needed?

### Nguồn gốc: Changes — đoạn 1020

No

### Nguồn gốc: Changes — đoạn 1021

9.

### Nguồn gốc: Submit data — đoạn 1022

electronically

### Nguồn gốc: Submit data — đoạn 1023

workflows

### Nguồn gốc: Health — đoạn 1024

facility

### Nguồn gốc: Yes — đoạn 1025

8.

### Nguồn gốc: Provide — đoạn 1026

feedback

### Nguồn gốc: Provide — đoạn 1027

11.

### Nguồn gốc: Any — đoạn 1028

feedback?

### Nguồn gốc: Any — đoạn 1029

No

### Nguồn gốc: Any — đoạn 1030

decisions

### Nguồn gốc: Yes — đoạn 1031

12.

### Nguồn gốc: Provide — đoạn 1032

feedback to facility

### Nguồn gốc: Provide — đoạn 1033

indicators

### Nguồn gốc: District health officer/staff — đoạn 1034

data

### Nguồn gốc: District health officer/staff — đoạn 1035

10.

### Nguồn gốc: Review — đoạn 1036

submitted data

### Nguồn gốc: Review — đoạn 1037

For key, see Table 6 (page 30). Source: PATH (13). a

### Nguồn gốc: Review — đoạn 1038

requirements

### Nguồn gốc: recommendations — đoạn 1039

personas scenarios workflows

### Nguồn gốc: 1. Check data quality — đoạn 1040

» Health-care facility reviews accuracy, validity and completeness of data in system.

### Nguồn gốc: 2. Correct fixable data quality issues — đoạn 1041

» Depending on local policy this step might not be required.

### Nguồn gốc: 3. Generate aggregate reports — đoạn 1042

» This can be automated and done digitally.

### Nguồn gốc: 4. Check aggregate reports — đoạn 1043

» Depending on local policy this step might not be required.

### Nguồn gốc: 5. Facility supervisor/in-charge review needed? — đoạn 1044

» Determine whether the report needs to be reviewed by the person in charge of the facility.

### Nguồn gốc: 6. Review report — đoạn 1045

» The reports are reviewed by facility supervisor/in-charge.

### Nguồn gốc: 7. Changes needed? — đoạn 1046

» Determine whether the report is accurate or has any issues.

### Nguồn gốc: 7. Changes needed? — đoạn 1047

requirements

### Nguồn gốc: 7. Changes needed? — đoạn 1048

indicators

### Nguồn gốc: 7. Changes needed? — đoạn 1049

decisions

### Nguồn gốc: 7. Changes needed? — đoạn 1050

data

### Nguồn gốc: 8. Provide feedback — đoạn 1051

» If there is any issue with the report, the person in charge will provide feedback to the responsible person to make corrections to client-level data.

### Nguồn gốc: 9. Submit data electronically — đoạn 1052

» Reports and data may be used by the facility supervisor/in-charge or staff at multiple points during the business process, such as here, earlier in the process or outside of the business process. »

### Nguồn gốc: 9. Submit data electronically — đoạn 1053

This can be automated and done digitally.

### Nguồn gốc: 9. Submit data electronically — đoạn 1054

»

### Nguồn gốc: 9. Submit data electronically — đoạn 1055

Depending on the local policies, an active “submission” may not be needed, and the district-, provincial- and national-level MOH would be able to access data directly for reporting purposes.

### Nguồn gốc: 10. Review submitted data — đoạn 1056

» Use data in report to review progress and make decisions on improvements and actions to take.

### Nguồn gốc: 11. Any feedback? — đoạn 1057

» Determine whether there is any feedback after review of the data.

### Nguồn gốc: 12. Provide feedback to facility — đoạn 1058

» If there is any feedback, the focal person will provide the feedback to the facility. If there are errors, the facility may be required to restart the process and resubmit.

### Nguồn gốc: 4.3 Additional considerations for adapting workflows — đoạn 1059

Although these workflows can be considered as a starting point, it is helpful to conduct further validation through interviews with the targeted personas or shadowing their work to obtain a better sense of the differences that would need to be reflected in the digital system. The WHO Handbook for digitalizing primary health care (17) can help in the development of new workflows or revisions to existing workflows.

### Nguồn gốc: Core data — đoạn 1060

elements

### Nguồn gốc: Core data — đoạn 1061

personas

### Nguồn gốc: Component — đoạn 1062

workflows

### Nguồn gốc: 5.1 Simplified list of core data elements — đoạn 1063

scenarios

### Nguồn gốc: 5.1 Simplified list of core data elements — đoạn 1064

This section outlines the core set of data elements corresponding to different points of the workflow within the identified business processes. These data elements provide the foundation for executing the decision-support logic, as well as populating indicators and performance metrics. Although this section provides a high-level overview of included data elements, a more complete “data dictionary” (in spreadsheet form) detailing the input options, calculations, validation checks and linked to standardized terminology codes, such as ICD and SNOMED, is available in Web Annex A. This data dictionary also includes data elements for configuring the decision support logic according to parameters, such as prevalence of certain conditions (e.g.

### Nguồn gốc: 5.1 Simplified list of core data elements — đoạn 1065

malaria, HIV prevalence, etc.) and health system characteristics (e.g. availability of ultrasound and other commodities).

### Nguồn gốc: 5.1 Simplified list of core data elements — đoạn 1066

data

### Nguồn gốc: 5.1 Simplified list of core data elements — đoạn 1067

Note: The data needs for the registration process have been combined with the data needs within the ANC contact process as these may often be done in tandem by the same health-care provider.

### Nguồn gốc: 5.1 Simplified list of core data elements — đoạn 1068

decisions

### Nguồn gốc: 5.1 Simplified list of core data elements — đoạn 1069

Table 7 provides a simplified list of core data elements and is merely a snapshot of the comprehensive data dictionary found in Web Annex A. As with the workflows, we view this data dictionary as “80% generic” with the expectation that the “other 20%” will be supplemented and modified through country adaptation.

### Nguồn gốc: 5.1 Simplified list of core data elements — đoạn 1070

indicators

### Nguồn gốc: 5.1 Simplified list of core data elements — đoạn 1071

requirements

### Nguồn gốc: recommendations — đoạn 1072

personas

### Nguồn gốc: recommendations — đoạn 1073

Activity ID.

### Nguồn gốc: Rapid assessment — đoạn 1074

and management

### Nguồn gốc: Urgent referral — đoạn 1075

needed

### Nguồn gốc: Unique identification — đoạn 1076

Unique identifier generated for new clients or a universal ID, if used in the country

### Nguồn gốc: Age — đoạn 1077

Age (number of years) of the client based on the DOB

### Nguồn gốc: Address — đoạn 1078

Client’s home address or address that the client is consenting to disclose

### Nguồn gốc: Woman wants to receive — đoạn 1079

reminders during pregnancy? (optional)

### Nguồn gốc: Woman wants to receive — đoạn 1080

Whether or not the woman wants to receive SMS or other messages regarding her ANC contacts and health status during pregnancy

### Nguồn gốc: Alternative contact’s name — đoạn 1081

Name of an alternative contact, which could be next of kin (e.g. partner, mother, sibling); the alternative contact would be used in the case of an emergency situation

### Nguồn gốc: Alternative contact’s phone — đoạn 1082

number

### Nguồn gốc: Co-habitants — đoạn 1083

Who the client lives with (e.g. parents [in cases of adolescents], partner, extended family, siblings, friend[s], no one)

### Nguồn gốc: Co-habitants — đoạn 1084

data decisions indicators requirements

### Nguồn gốc: Business process ANC.A: Registration — đoạn 1085

workflows

### Nguồn gốc: Business process ANC.A: Registration — đoạn 1086

scenarios

### Nguồn gốc: Business process ANC.A: Registration — đoạn 1087

Table 7. Workflow core data elements for identified business processes

### Nguồn gốc: Validate client — đoạn 1088

details

### Nguồn gốc: Description and definition — đoạn 1089

personas

### Nguồn gốc: recommendations — đoạn 1090

Activity ID.

### Nguồn gốc: Rapid assessment — đoạn 1091

and management

### Nguồn gốc: Reason for coming to facility — đoạn 1092

Records the reason why the woman came to the health-care facility today

### Nguồn gốc: Specific health concern(s) — đoạn 1093

If the woman came to the facility with a specific health concern, select the health concern(s) from the list

### Nguồn gốc: Danger signs — đoạn 1094

Before each contact, the health worker should check whether the woman has any of the danger signs listed here – if yes, she should refer to the hospital urgently; if no, she should continue to the normal contact

### Nguồn gốc: Danger signs — đoạn 1095

scenarios

### Nguồn gốc: Arrive at facility — đoạn 1096

workflows data decisions indicators

### Nguồn gốc: Arrive at facility — đoạn 1097

requirements

### Nguồn gốc: Collect women’s — đoạn 1098

history and profile

### Nguồn gốc: Highest level of education — đoạn 1099

achieved

### Nguồn gốc: Gestational age (GA) — đoạn 1100

GA in weeks and/or days depending on the source of gestational age

### Nguồn gốc: Source of gestational age — đoạn 1101

How the gestational age was calculated (e.g. last menstrual period [LMP], ultrasound, symphysis-fundal height [SFH] or abdominal palpation)

### Nguồn gốc: Number of pregnancies (gravida) — đoạn 1102

Total number of times the woman has been pregnant (including this pregnancy) and their outcomes

### Nguồn gốc: Whether last live birth was — đoạn 1103

preterm

### Nguồn gốc: Whether last live birth was — đoạn 1104

Was the last live birth preterm? Whether the last live birth was preterm (i.e. less than 37 weeks gestation at the time of delivery)

### Nguồn gốc: Past pregnancy complications — đoạn 1105

Mark whether the woman has had any complications or problems in any previous pregnancy

### Nguồn gốc: Substance use during past — đoạn 1106

pregnancy

### Nguồn gốc: Tetanus toxoid-containing vaccine — đoạn 1107

(TTCV) immunization history

### Nguồn gốc: Flu immunization history — đoạn 1108

Whether or not this year’s seasonal flu vaccine has been provided

### Nguồn gốc: Flu immunization history — đoạn 1109

decisions

### Nguồn gốc: Daily caffeine intake — đoạn 1110

Assesses whether the woman consumes more than 300 mg of caffeine per day

### Nguồn gốc: Current alcohol and/or substance — đoạn 1111

use

### Nguồn gốc: Current alcohol and/or substance — đoạn 1112

Whether or not the woman currently consumes any alcohol or substances

### Nguồn gốc: Current alcohol and/or substance — đoạn 1113

indicators

### Nguồn gốc: HIV status of the woman’s partner (reported) — đoạn 1114

requirements

### Nguồn gốc: HIV status of the woman’s partner (reported) — đoạn 1115

data

### Nguồn gốc: HIV status of the woman’s partner (reported) — đoạn 1116

workflows

### Nguồn gốc: HIV status of the woman’s partner (reported) — đoạn 1117

scenarios

### Nguồn gốc: HIV status of the woman’s partner (reported) — đoạn 1118

personas

### Nguồn gốc: HIV status of the woman’s partner (reported) — đoạn 1119

Activity ID.

### Nguồn gốc: Symptoms and — đoạn 1120

follow-up

### Nguồn gốc: Intimate partner violence (IPV) — đoạn 1121

signs and symptoms

### Nguồn gốc: Fetal movement — đoạn 1122

Whether the woman has felt the baby move or not or if the baby’s movements have decreased

### Nguồn gốc: Height and weight — đoạn 1123

Current height in centimetres, pre-gestational weight in kilograms, and current weight in kilograms, and calculation of body mass index (BMI)

### Nguồn gốc: Expected weight gain — đoạn 1124

Expected weight gain during pregnancy is based on the woman’s weight category

### Nguồn gốc: Blood pressure — đoạn 1125

Woman’s systolic and diastolic blood pressure (mmHg), or reason blood pressure cannot be taken

### Nguồn gốc: Breast exam result — đoạn 1126

Whether or not the result of the breast exam is normal

### Nguồn gốc: Abdominal exam result — đoạn 1127

Whether or not the result of the abdominal exam is normal

### Nguồn gốc: Pelvic exam result — đoạn 1128

Whether or not the result of the visual pelvic exam is normal

### Nguồn gốc: Pelvic exam result — đoạn 1129

workflows data decisions indicators

### Nguồn gốc: Pelvic exam result — đoạn 1130

requirements

### Nguồn gốc: Core data elements — đoạn 1131

scenarios

### Nguồn gốc: Physical exam — đoạn 1132

personas

### Nguồn gốc: Physical exam — đoạn 1133

Activity ID.

### Nguồn gốc: Oedema present — đoạn 1134

Whether or not the woman has oedema, and if so the type of oedema

### Nguồn gốc: Fetal assessment — đoạn 1135

Symphus-fundal height, whether health worker observes a fetal heartbeat, and record heart rate

### Nguồn gốc: Fetal presentation — đoạn 1136

If a single fetus only, indicate the presentation of the fetus in the uterus

### Nguồn gốc: IPV — đoạn 1137

Signs or conditions (based on physical exam) that trigger suspicion of IPV

### Nguồn gốc: Clinical enquiry for IPV — đoạn 1138

Whether or not clinical enquiry for IPV was conducted based on presenting signs and symptoms and conditions

### Nguồn gốc: What type(s) of violence has the woman been subjected to — đoạn 1139

requirements

### Nguồn gốc: What type(s) of violence has the woman been subjected to — đoạn 1140

indicators

### Nguồn gốc: What type(s) of violence has the woman been subjected to — đoạn 1141

decisions

### Nguồn gốc: What type(s) of violence has the woman been subjected to — đoạn 1142

data

### Nguồn gốc: What type(s) of violence has the woman been subjected to — đoạn 1143

workflows

### Nguồn gốc: What type(s) of violence has the woman been subjected to — đoạn 1144

scenarios

### Nguồn gốc: What type(s) of violence has the woman been subjected to — đoạn 1145

personas

### Nguồn gốc: What type(s) of violence has the woman been subjected to — đoạn 1146

Activity ID.

### Nguồn gốc: Conduct laboratory — đoạn 1147

tests and imaging

### Nguồn gốc: Ultrasound scan — đoạn 1148

Whether ultrasound scan is required, has been ordered or date performed; or reason why required ultrasound scan was not done

### Nguồn gốc: Blood type — đoạn 1149

Whether a blood type test was ordered and the woman’s blood type

### Nguồn gốc: HIV status — đoạn 1150

Whether an HIV test was done, the date of the HIV test, test result and diagnosis; or reason

### Nguồn gốc: Partner HIV status — đoạn 1151

Whether partner’s HIV test was done or status is known, test result and diagnosis; or reason

### Nguồn gốc: Hepatitis B — đoạn 1152

Whether hepatitis B test was done, type of test, test result and diagnosis; or reason hepatitis

### Nguồn gốc: Hepatitis C — đoạn 1153

Whether hepatitis C test was done, type of test, test result and diagnosis; or reason hepatitis

### Nguồn gốc: Syphilis — đoạn 1154

Whether syphilis test was done, type of test, test result and diagnosis; or reason syphilis test was not done

### Nguồn gốc: Urine — đoạn 1155

Whether urine test was done, type of test, test result (leukocytes, nitrites, protein), and asymptomatic bacteria diagnosis; or reason urine test was not conducted

### Nguồn gốc: Blood glucose and diabetes — đoạn 1156

diagnosis

### Nguồn gốc: Blood glucose and diabetes — đoạn 1157

Whether blood glucose test was done, type of test, test result, and gestational diabetes mellitus (GDM)/diabetes mellitus (DM) in pregnancy diagnosis; or reason the blood glucose test was not done

### Nguồn gốc: Blood haemoglobin — đoạn 1158

Whether blood haemoglobin test was done, type of test, test result, and anaemia diagnosis; or reason blood haemoglobin test was not conducted

### Nguồn gốc: Tuberculosis (TB) screening — đoạn 1159

Whether TB screening was done, TB screening results; or reason TB screening was not done

### Nguồn gốc: Self-care interventions in the home or — đoạn 1160

community

### Nguồn gốc: Core data elements — đoạn 1161

requirements

### Nguồn gốc: Core data elements — đoạn 1162

See Decision-support scheduling logic in Component 6 and Web Annex B

### Nguồn gốc: Core data elements — đoạn 1163

indicators

### Nguồn gốc: Scheduling follow-up contact — đoạn 1164

decisions

### Nguồn gốc: See ANC.C: Referral — đoạn 1165

data

### Nguồn gốc: Referral needed? — đoạn 1166

workflows

### Nguồn gốc: See decision-support tables in Component 6 and Web Annex B — đoạn 1167

scenarios

### Nguồn gốc: Counselling and treatment — đoạn 1168

personas

### Nguồn gốc: Counselling and treatment — đoạn 1169

Activity ID.

### Nguồn gốc: Provide information — đoạn 1170

to referral facility

### Nguồn gốc: Reason for referral — đoạn 1171

Reason why the client is being referred; if diagnosed, this may include the diagnosis

### Nguồn gốc: Provider’s facility and phone — đoạn 1172

number

### Nguồn gốc: Provider’s facility and phone — đoạn 1173

Facility client is being referred from, and contact details of the provider making the referral

### Nguồn gốc: Referral notes — đoạn 1174

Any additional relevant details of clinical significance for the referral facility to provide continuity of care

### Nguồn gốc: Referral notes — đoạn 1175

N/A, not applicable.

### Nguồn gốc: Referral notes — đoạn 1176

requirements

### Nguồn gốc: Referral notes — đoạn 1177

indicators

### Nguồn gốc: Referral notes — đoạn 1178

decisions

### Nguồn gốc: Referral notes — đoạn 1179

data

### Nguồn gốc: Referral notes — đoạn 1180

workflows

### Nguồn gốc: Business process ANC.C: Referral — đoạn 1181

scenarios

### Nguồn gốc: Business process ANC.C: Referral — đoạn 1182

personas

### Nguồn gốc: Business process ANC.C: Referral — đoạn 1183

Activity ID.

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1184

The section above outlines the core data elements that should be included in digital systems to facilitate the decision-support logic or indicators. There are additional derived data elements that are based on calculations of the core data elements above (Table 8).

### Nguồn gốc: Core data elements used for calculation — đoạn 1185

(i.e. the “variables”)

### Nguồn gốc: ANC.B6.DE19. Last menstrual period (LMP) — đoạn 1186

([Today’s date] – LMP) / 7

### Nguồn gốc: ANC.B8.DE1. Height — đoạn 1187

([Pre-gestational weight (kg)] / [{Height (centimetres)/100}^2])

### Nguồn gốc: ANC.B8.DE2. Pre-gestational weight — đoạn 1188

scenarios

### Nguồn gốc: Calculated data element label — đoạn 1189

personas

### Nguồn gốc: Calculated data element label — đoạn 1190

Table 8. Derived data elements

### Nguồn gốc: ANC.B8.DE3. Current weight — đoạn 1191

(Current weight (kg)) – ([Pre-gestational weight (kg)] OR [Current weight at first contact])

### Nguồn gốc: ANC.B8.DE3. Current weight — đoạn 1192

workflows

### Nguồn gốc: Total weight gain (kg) — đoạn 1193

Table 9.

### Nguồn gốc: Description — đoạn 1194

indicators

### Nguồn gốc: Population prevalence of undernourishment — đoạn 1195

The proportion of women in the adult population (18 years or older) with a BMI less than 18.5

### Nguồn gốc: Population prevalence of anaemia — đoạn 1196

The proportion of pregnant women in the population with anaemia (haemoglobin level less than 11 g/dl)

### Nguồn gốc: Population with low dietary calcium intake — đoạn 1197

Women in the population are likely to have low dietary calcium intake (less than 900 mg of calcium per day)

### Nguồn gốc: Population prevalence of tuberculosis (TB) — đoạn 1198

The tuberculosis prevalence in the general population in number of cases per 100 000 persons or greater

### Nguồn gốc: Population prevalence of tuberculosis (TB) — đoạn 1199

requirements

### Nguồn gốc: Core data elements — đoạn 1200

decisions

### Nguồn gốc: Core data elements — đoạn 1201

Some settings may require the inclusion of additional data elements into the full dataset or changes to response options based on contextual differences. Additionally, the transition from paper-based forms to digital systems may require some reflection on whether data elements currently on the paper forms should be incorporated into the digital system. The following table provides an initial list of characteristics anticipated for each implementation to review and customize based on the national guidelines and local context (see Table 9).

### Nguồn gốc: Core data elements — đoạn 1202

data

### Nguồn gốc: recommendations — đoạn 1203

personas scenarios workflows

### Nguồn gốc: Population with vitamin A deficiency — đoạn 1204

Vitamin A deficiency is a severe public health problem if 5% or more of women in a population have a history of night blindness in their most recent pregnancy in the previous 3–5 years that ended in a live birth, or if 20% or more of pregnant women have a serum retinol level below 0.70 µmol/L

### Nguồn gốc: Population prevalence of soil-transmitted — đoạn 1205

helminth infection

### Nguồn gốc: Population prevalence of soil-transmitted — đoạn 1206

The percentage of individuals in the general population infected with at least one species of soil-transmitted helminth

### Nguồn gốc: Population prevalence of HIV in pregnant women — đoạn 1207

The proportion of pregnant women in the population who are HIV positive

### Nguồn gốc: Population prevalence of HIV in key populations — đoạn 1208

(men who have sex with men, people in prison or other closed settings, people who inject drugs, sex workers and transgender people)

### Nguồn gốc: Population prevalence of HIV in key populations — đoạn 1209

The proportion of individuals in each of the key populations who are HIV positive

### Nguồn gốc: Population prevalence of syphilis in pregnant — đoạn 1210

women

### Nguồn gốc: Population prevalence of hepatitis C — đoạn 1211

The proportion of hepatitis C virus antibody seroprevalance in the general population

### Nguồn gốc: Minimum requirements for IPV assessment — đoạn 1212

decisions

### Nguồn gốc: Minimum requirements for IPV assessment — đoạn 1213

data

### Nguồn gốc: Site-specific characteristics — đoạn 1214

WHO does not recommend universal screening for violence of women attending health services. WHO does encourage health-care providers to raise the topic only with women who have injuries or conditions that they suspect may be related to violence. All of the following must be in place at the health facility for this to be TRUE: a. A protocol or standard operating procedure for intimate partner violence (IPV) b. A health worker trained on how to ask about IPV and how to provide the minimum response or beyond c. A private setting d. A way to ensure confidentiality

### Nguồn gốc: Site-specific characteristics — đoạn 1215

requirements

### Nguồn gốc: Site-specific characteristics — đoạn 1216

indicators

### Nguồn gốc: Site-specific characteristics — đoạn 1217

e. Time to allow for appropriate disclosure AND f. A system for referral in place.

### Nguồn gốc: Ultrasound available at the health-care facility — đoạn 1218

Is an ultrasound machine available and functional at your facility and a trained health worker available to use it?

### Nguồn gốc: Decisionsupport logic — đoạn 1219

personas

### Nguồn gốc: Component — đoạn 1220

data

### Nguồn gốc: Component — đoạn 1221

This section first provides an overview and inventory of all the decision-support tables (Table 10) across the ANC contact workflow. The listed decisionsupport tables are expanded in greater detail to specify the inputs and conditions that would result in a decision output and action, such as a referral, treatment or counselling. The structure of the decision-support tables is based on an adaptation of the Decision Model Notation (DMN), an industry standard for modelling and executing decision-support logics (25). The DMN format of the decision-support tables reflects IF/THEN statements.

### Nguồn gốc: Component — đoạn 1222

Tables 11 and 12 provide examples of the expanded decision-support tables; the full set of decision-support tables is found in Web Annex B.

### Nguồn gốc: Component — đoạn 1223

workflows

### Nguồn gốc: 6.1 Decision-support logic overview — đoạn 1224

scenarios

### Nguồn gốc: 6.1 Decision-support logic overview — đoạn 1225

The decision-support logic component of the adaptation kit provides the decision-support logics and algorithms, as well as the scheduling of services, in accordance with WHO guidelines. In this DAK, the decision-support logics and algorithms deconstruct the recommendations within the WHO ANC guidelines into a format that clearly labels the inputs and outputs, along with any actions required, that would be operationalized in a digital decisionsupport system.

### Nguồn gốc: 6.1 Decision-support logic overview — đoạn 1226

decisions indicators

### Nguồn gốc: 6.1 Decision-support logic overview — đoạn 1227

requirements

### Nguồn gốc: Decisionsupport — đoạn 1228

table ID

### Nguồn gốc: Pregnancy, childbirth, postpartum and newborn care guide (IMPAC) — đoạn 1229

(2015): B2 (26)

### Nguồn gốc: HEADSS — đoạn 1230

assessment

### Nguồn gốc: Conduct HEADSS assessment if adolescent — đoạn 1231

Adolescent job aid: a handy desk reference tool for primary level health workers. (2010) (27)

### Nguồn gốc: Check — đoạn 1232

symptoms

### Nguồn gốc: Check symptoms and follow up on previous symptoms. Symptoms — đoạn 1233

include: – non-pharmacological measures to relieve nausea and vomiting – pharmacological treatments for nausea and vomiting – heartburn – leg cramps – low back and pelvic pain – constipation – varicose veins and oedema counselling

### Nguồn gốc: Check symptoms and follow up on previous symptoms. Symptoms — đoạn 1234

WHO ANC recommendations (2016) (8) WHO consolidated self-care interventions guidelines (2019) (29)

### Nguồn gốc: Pregnancy, childbirth, postpartum and newborn care guide (IMPAC) — đoạn 1235

(2015) (26)

### Nguồn gốc: Conduct — đoạn 1236

physical exam

### Nguồn gốc: ANC.DT.04 — đoạn 1237

Conduct physical exams: depending on the output of the first reading, additional examination may be needed, including:

### Nguồn gốc: ANC.DT.04 — đoạn 1238

WHO ANC recommendations (2016): D.1–D.6 (8) Managing complications guide (IMPAC) (2017) (28)

### Nguồn gốc: Pregnancy, childbirth, postpartum and newborn care guide (IMPAC) — đoạn 1239

(2015) – B2 (26)

### Nguồn gốc: Physical exam — đoạn 1240

referral

### Nguồn gốc: Physical symptoms and exam results requiring referral — đoạn 1241

Managing complications guide (IMPAC) (2017) (28)

### Nguồn gốc: Laboratory & — đoạn 1242

imaging

### Nguồn gốc: Ultrasound recommendation — đoạn 1243

WHO ANC recommendations (2016): B.2.4 (8)

### Nguồn gốc: HIV testing — đoạn 1244

WHO ANC recommendations (2016): B.1.7 (8)

### Nguồn gốc: Hepatitis B testing — đoạn 1245

Guidelines on hepatitis B and C testing (2017): Recommendation 6.1 (31)

### Nguồn gốc: Hepatitis C testing — đoạn 1246

Guidelines on hepatitis B and C testing (2017): Recommendation 6.1 (31)

### Nguồn gốc: Syphilis testing — đoạn 1247

WHO ANC recommendations (2016): B.1.7 (8)

### Nguồn gốc: Urine testing — đoạn 1248

Managing complications guide (IMPAC) (2017) (28) WHO ANC recommendations (2016) (8)

### Nguồn gốc: Tuberculosis screening — đoạn 1249

WHO ANC recommendations (2016): B.1.8 (8)

### Nguồn gốc: Other — đoạn 1250

Managing complications guide (IMPAC) (2017) (28)

### Nguồn gốc: Other — đoạn 1251

requirements

### Nguồn gốc: Other — đoạn 1252

indicators

### Nguồn gốc: Other — đoạn 1253

decisions

### Nguồn gốc: Other — đoạn 1254

data

### Nguồn gốc: Other — đoạn 1255

workflows

### Nguồn gốc: Other — đoạn 1256

scenarios

### Nguồn gốc: Other — đoạn 1257

personas

### Nguồn gốc: Other — đoạn 1258

Table 10. Overview of decision-support tables for ANC contact workflow (Workflow B)

### Nguồn gốc: Decisionsupport — đoạn 1259

table ID

### Nguồn gốc: Behaviour — đoạn 1260

counselling

### Nguồn gốc: ANC.DT.15 — đoạn 1261

Behaviour counselling including: – caffeine reduction counselling – tobacco cessation counselling – second-hand smoke counselling – condom counselling – alcohol/substance use counselling

### Nguồn gốc: ANC.DT.15 — đoạn 1262

WHO ANC recommendations (2016): A.10, B.1.5, B.1.6 (8)

### Nguồn gốc: Dietary — đoạn 1263

counselling

### Nguồn gốc: Recommended dietary counselling for all women, with additional — đoạn 1264

counselling required for underweight women

### Nguồn gốc: Recommended dietary counselling for all women, with additional — đoạn 1265

WHO ANC recommendations (2016): A.1, A.5–9 (8)

### Nguồn gốc: Diagnosis & — đoạn 1266

treatment

### Nguồn gốc: Pregnancy, childbirth, postpartum and newborn care guide (IMPAC) — đoạn 1267

(2015): C3 (26) Managing complications guide (IMPAC) (2017): S-53, S-61 (28) WHO pre-eclampsia and eclampsia recommendations (2011): 4, 6, 7 (34)

### Nguồn gốc: HIV diagnosis — đoạn 1268

WHO ANC recommendations (2016): B.1.7 (8) Consolidated guidelines on HIV testing services (2015) (30) Consolidated guideline on SRHR women with HIV (2017) (33)

### Nguồn gốc: Hepatitis B diagnosis — đoạn 1269

Guideline on hepatitis B and C testing (2017): Sections 16.6, 16.7 (31)

### Nguồn gốc: Hepatitis C diagnosis — đoạn 1270

Guideline on hepatitis B and C testing (2017): Section 16.5 (31)

### Nguồn gốc: Syphilis diagnosis — đoạn 1271

WHO ANC recommendations (2016): B.1.7 (8)

### Nguồn gốc: Asymptomatic bacteriuria (ASB) diagnosis — đoạn 1272

WHO ANC recommendations (2016): B.1.2 (8)

### Nguồn gốc: Gestational diabetes mellitus (GDM) and diabetes mellitus (DM) during — đoạn 1273

pregnancy diagnosis

### Nguồn gốc: Gestational diabetes mellitus (GDM) and diabetes mellitus (DM) during — đoạn 1274

WHO ANC recommendations (2016): B.1.4 (8)

### Nguồn gốc: Tuberculosis (TB) diagnosis — đoạn 1275

WHO ANC recommendations (2016): B.1.8 (8)

### Nguồn gốc: Iron and folic acid supplementation recommended for treatment of — đoạn 1276

anaemia and/or standard nutritional supplementation

### Nguồn gốc: Iron and folic acid supplementation recommended for treatment of — đoạn 1277

WHO ANC recommendations (2016): A.2.1–5, B.1.1 (8)

### Nguồn gốc: Pregnancy, childbirth, postpartum and newborn care guide (IMPAC) — đoạn 1278

(2015): C4 (26)

### Nguồn gốc: Calcium and vitamin A supplementation — đoạn 1279

WHO ANC recommendations (2016): A.3, A.4 (8)

### Nguồn gốc: Calcium and vitamin A supplementation — đoạn 1280

personas

### Nguồn gốc: Task — đoạn 1281

scenarios workflows data decisions indicators

### Nguồn gốc: Task — đoạn 1282

requirements

### Nguồn gốc: Decisionsupport — đoạn 1283

table ID

### Nguồn gốc: Risk reduction — đoạn 1284

counselling

### Nguồn gốc: Pregnancy, childbirth, postpartum and newborn care guide (IMPAC) — đoạn 1285

(2015): C3 (26) WHO pre-eclampsia and eclampsia recommendations (2011): 1–25 (34)

### Nguồn gốc: Gestational diabetes mellitus (GDM) risk counselling — đoạn 1286

WHO ANC recommendations (2016): B.1.4–10 (8)

### Nguồn gốc: HIV risk counselling — đoạn 1287

WHO ANC recommendations (2016): B.1.7, C.1 (8) Pregnancy, childbirth, postpartum and newborn care guide (2015): C4 (26)

### Nguồn gốc: General risk reduction counselling — đoạn 1288

WHO ANC recommendations (2016): C.1, C.3 (8) Managing complications guide (IMPAC) (2017): S-11, S-15 (28)

### Nguồn gốc: Pregnancy, childbirth, postpartum and newborn care guide (IMPAC) — đoạn 1289

(2015) (26)

### Nguồn gốc: Recommended — đoạn 1290

immunizations during pregnancy

### Nguồn gốc: Flu immunization — đoạn 1291

Vaccines against flu position paper (2012) (35)

### Nguồn gốc: Tetanus toxoid immunization — đoạn 1292

WHO ANC recommendations (2016): C.5 (8) Maternal immunization against tetanus (2006) (36)

### Nguồn gốc: General — đoạn 1293

counselling

### Nguồn gốc: Pregnancy, childbirth, postpartum and newborn care guide (IMPAC) — đoạn 1294

(2015): K2 (26) WHO recommendations on intrapartum care (2018): Recommendation 49 (37)

### Nguồn gốc: Pregnancy, childbirth, postpartum and newborn care guide (IMPAC) — đoạn 1295

(2015): C16 (26) Medical eligibility criteria for contraceptive use (2015) (38)

### Nguồn gốc: Pregnancy, childbirth, postpartum and newborn care guide (IMPAC) — đoạn 1296

(2015): C14 (26) WHO recommendations on health promotion interventions (2015):

### Nguồn gốc: WHO ANC recommendations: B1.3 (8) — đoạn 1297

IPV handbook (2014) (40) Responding to IPV (2013) (41)

### Nguồn gốc: WHO ANC recommendations: B1.3 (8) — đoạn 1298

IPV handbook (2014) (40) Responding to IPV (2013) (41)

### Nguồn gốc: Deworming and malaria prophylaxis — đoạn 1299

WHO ANC recommendations (2016): C.4, C.6 (8)

### Nguồn gốc: Deworming and malaria prophylaxis — đoạn 1300

requirements

### Nguồn gốc: Deworming and malaria prophylaxis — đoạn 1301

indicators

### Nguồn gốc: Deworming and malaria prophylaxis — đoạn 1302

decisions

### Nguồn gốc: Deworming and malaria prophylaxis — đoạn 1303

data

### Nguồn gốc: Deworming and malaria prophylaxis — đoạn 1304

workflows

### Nguồn gốc: Deworming and malaria prophylaxis — đoạn 1305

scenarios

### Nguồn gốc: Deworming and malaria prophylaxis — đoạn 1306

personas

### Nguồn gốc: Intimate — đoạn 1307

partner violence (IPV)

### Nguồn gốc: Deworming & — đoạn 1308

malaria

### Nguồn gốc: 6.2 Decision-support tables — đoạn 1309

Each of the decision-support logics listed in the overview table (Table 10) is elaborated in the linked web appendix. These decision-support tables comprise of the following components described in Table 11.

### Nguồn gốc: 6.2 Decision-support tables — đoạn 1310

personas

### Nguồn gốc: 6.2 Decision-support tables — đoạn 1311

Table 11. Components of the decision-support tables

### Nguồn gốc: Business rule — đoạn 1312

The description of the decision that needs to be made based on IF/THEN statements with the appropriate data element name for the variables. The rule should demonstrate the relationship between the input variables and the expected outputs and actions within the decision-support logic – e.g. if blood pressure is higher than 140 SBP/90 DBP, then the client should be flagged as a high-risk pregnancy.

### Nguồn gốc: Trigger — đoạn 1313

The event that would indicate when this decision-support logic should appear within the workflow, such as the activity that would trigger this decision to be made.

### Nguồn gốc: These are the variables or conditions that — đoạn 1314

need to be considered to determine the consequent actions or outputs

### Nguồn gốc: The resulting action or decision based on — đoạn 1315

the combination of input entries. This is the statement that immediately follows the “THEN”. Examples of outputs may include a diagnosis, alerts/prompts for referral, and counselling.

### Nguồn gốc: Concrete measures to be taken based on — đoạn 1316

the output (refer, provide treatment and/ or counselling, conduct test, etc.). In some cases, output and action may be the same.

### Nguồn gốc: Additional explanations or descriptions, — đoạn 1317

including possible pop-up alert messages and relevant background information.

### Nguồn gốc: Additional explanations or descriptions, — đoạn 1318

decisions

### Nguồn gốc: Additional explanations or descriptions, — đoạn 1319

» Inputs placed on different rows are considered as “OR” conditions that can be considered independently of the inputs on other rows.

### Nguồn gốc: This section can also include the written — đoạn 1320

content which would appear in the pop-up messages notifying the health worker on the appropriate next steps, which can include counselling, case management approach, or referral.

### Nguồn gốc: This section can also include the written — đoạn 1321

data

### Nguồn gốc: This section can also include the written — đoạn 1322

» If there are multiple input entries on the same row, these different inputs are considered as “AND” – conditions that need to be in place at the same time

### Nguồn gốc: This section can also include the written — đoạn 1323

workflows

### Nguồn gốc: This section can also include the written — đoạn 1324

The name of the “decision” describing what algorithm or logic is represented (e.g. pre-eclampsia risk counselling). The Decision ID should correspond to the number in the overview table (Table 10).

### Nguồn gốc: This section can also include the written — đoạn 1325

scenarios

### Nguồn gốc: Decision ID — đoạn 1326

DBP: diastolic blood pressure; SBP: systolic blood pressure.

### Nguồn gốc: Decision ID — đoạn 1327

indicators

### Nguồn gốc: Decision ID — đoạn 1328

requirements

### Nguồn gốc: recommendations — đoạn 1329

personas scenarios workflows

### Nguồn gốc: recommendations — đoạn 1330

Below is an excerpt from an elaborated decision-support table using an example from ANC. All the decision-support tables referenced above in the overview table (Table 10) are available in the Web Annex B.

### Nguồn gốc: Business Rule — đoạn 1331

If the woman presents with any of the following symptoms and/or test results, conduct counselling and referral as needed

### Nguồn gốc: Action — đoạn 1332

140 mmHg ≤ “Systolic blood pressure” < 160 mmHg

### Nguồn gốc: Action — đoạn 1333

140 mmHg ≤ “Repeat systolic blood pressure” < 160 mmHg

### Nguồn gốc: Action — đoạn 1334

“Urine dipstick result – “Symptoms of severe protein” = “++” pre-eclampsia” = “No symptoms of severe preeclampsia”

### Nguồn gốc: Action — đoạn 1335

“Pre-eclampsia” = TRUE Refer urgently to a hospital

### Nguồn gốc: Action — đoạn 1336

90 mmHg ≤ “Diastolic blood pressure” < 110 mmHg

### Nguồn gốc: Action — đoạn 1337

90 mmHg ≤ “Repeat diastolic blood pressure” < 110 mmHg

### Nguồn gốc: Action — đoạn 1338

“Urine dipstick result – “Symptoms of severe protein” = “++” pre-eclampsia” = “No symptoms of severe preeclampsia”

### Nguồn gốc: Action — đoạn 1339

“Pre-eclampsia” = TRUE Refer urgently to a hospital

### Nguồn gốc: Action — đoạn 1340

140 mmHg ≤ “Systolic blood pressure” < 160 mmHg

### Nguồn gốc: Action — đoạn 1341

140 mmHg ≤ “Repeat systolic blood pressure” < 160 mmHg

### Nguồn gốc: Action — đoạn 1342

“Urine dipstick result – “Symptoms of severe protein” = “+++” pre-eclampsia” = “No symptoms of severe preeclampsia”

### Nguồn gốc: Action — đoạn 1343

“Pre-eclampsia” = TRUE Refer urgently to a hospital

### Nguồn gốc: Action — đoạn 1344

90 mmHg ≤ “Diastolic blood pressure” < 110 mmHg

### Nguồn gốc: Action — đoạn 1345

90 mmHg ≤ “Repeat diastolic blood pressure” < 110 mmHg

### Nguồn gốc: Action — đoạn 1346

“Urine dipstick result – “Symptoms of severe protein” = “+++” pre-eclampsia” = “No symptoms of severe preeclampsia”

### Nguồn gốc: Action — đoạn 1347

“Pre-eclampsia” = TRUE Refer urgently to a hospital

### Nguồn gốc: Action — đoạn 1348

requirements

### Nguồn gốc: Action — đoạn 1349

indicators

### Nguồn gốc: Action — đoạn 1350

decisions

### Nguồn gốc: Action — đoạn 1351

data

### Nguồn gốc: Action — đoạn 1352

Table 12. Example decision-logic table for hypertension counselling

### Nguồn gốc: Refer to hospital and — đoạn 1353

revise birth plan.

### Nguồn gốc: Woman has preeclampsia – SBP of 140 — đoạn 1354

mmHg or above and/ or DBP of 90 mmHg or above and proteinuria 2+ and no symptom of severe pre-eclampsia. Procedure: – Refer to hospital – Revise the birth plan

### Nguồn gốc: Woman has preeclampsia – SBP of 140 — đoạn 1355

Decision trees may be used to supplement the structured format of the decision-support tables and can help in visualizing different pathways. The addition of decision trees may be especially useful for decision points that consist of multiple inputs and outputs. Examples of decision trees are given in Fig. 11.

### Nguồn gốc: Woman has preeclampsia – SBP of 140 — đoạn 1356

personas

### Nguồn gốc: Woman has preeclampsia – SBP of 140 — đoạn 1357

Fig. 11. Examples of decision trees to supplement decision-support tables

### Nguồn gốc: If the woman is or has — đoạn 1358

»

### Nguồn gốc: Keep the woman in — đoạn 1359

the waiting room for routine care

### Nguồn gốc: Risk assessment using — đoạn 1360

tool

### Nguồn gốc: Woman not at — đoạn 1361

substantial risk of HIV acquisition

### Nguồn gốc: Partner not — đoạn 1362

tested or

### Nguồn gốc: HIV-positive — đoạn 1363

Offer partner:

### Nguồn gốc: Woman at substantial — đoạn 1364

risk of HIV acquisition

### Nguồn gốc: Woman at substantial — đoạn 1365

» Reassess woman’s risk » Offer partner referral for

### Nguồn gốc: 1. HIV — đoạn 1366

treatment (if

### Nguồn gốc: 2. Condom — đoạn 1367

promotion

### Nguồn gốc: 3. Risk — đoạn 1368

reduction counselling

### Nguồn gốc: 3. Risk — đoạn 1369

Provide comprehensive HIV prevention options:

### Nguồn gốc: 4. PrEP with emphasis — đoạn 1370

treatment (syndromic on adherence and syphilis)

### Nguồn gốc: 2. Condom promotion — đoạn 1371

importance of follow-up ANC visits

### Nguồn gốc: 3. Risk reduction — đoạn 1372

counselling

### Nguồn gốc: 3. Risk reduction — đoạn 1373

indicators

### Nguồn gốc: 3. Emphasize importance of follow-up visits and — đoạn 1374

repeat HIV testing

### Nguồn gốc: 3. Emphasize importance of follow-up visits and — đoạn 1375

ANC: antenatal care; PMTCT: prevention of mother-to-child transmission; PrEP: pre-exposure prophylaxis; STI: sexually transmitted infection; VMMC: voluntary medical male circumcision.

### Nguồn gốc: Decision-support logic — đoạn 1376

requirements

### Nguồn gốc: Offer partner testing — đoạn 1377

& conduct risk assessment

### Nguồn gốc: Offer partner testing — đoạn 1378

decisions

### Nguồn gốc: Offer partner testing — đoạn 1379

» Transfer woman to a retreatment room for Rapid

### Nguồn gốc: Assessment and Management — đoạn 1380

» Call for help » Reassure the woman that she will be taken care of immediately » Ask her companion to stay

### Nguồn gốc: Standard HIV post-test guidance and — đoạn 1381

counselling on prevention

### Nguồn gốc: Standard HIV post-test guidance and — đoạn 1382

data

### Nguồn gốc: Standard HIV post-test guidance and — đoạn 1383

» » » » » »

### Nguồn gốc: Headache and visual — đoạn 1384

disturbance

### Nguồn gốc: Severe difficulty — đoạn 1385

breathing

### Nguồn gốc: PMTCT, treatment, support, — đoạn 1386

offer partner notification

### Nguồn gốc: PMTCT, treatment, support, — đoạn 1387

workflows

### Nguồn gốc: PMTCT, treatment, support, — đoạn 1388

» » » » » »

### Nguồn gốc: Provider-initiated HIV testing & counselling — đoạn 1389

scenarios

### Nguồn gốc: Danger Sign — đoạn 1390

Check:

### Nguồn gốc: recommendations — đoạn 1391

personas scenarios workflows

### Nguồn gốc: 6.4 Scheduling logic overview — đoạn 1392

In addition to specific decision-support logic that needs to be detailed, there is also scheduling logic to facilitate the digital tracking of clients and ensure that appropriate services are provided in a timely manner according to clinical protocols. For example, it will be important for the health worker to know when the client’s next contact is due based on the recommendations for eight scheduled contacts throughout the course of the pregnancy. In the case of ANC, there is a need to map out the schedules for upcoming contacts, immunizations and lab tests. Table 13 provides an overview of the different schedule logic tables included in this digital adaptation kit. The details within each scheduling logic are elaborated in Web Annex B.

### Nguồn gốc: 6.4 Scheduling logic overview — đoạn 1393

Table 13. Overview of scheduling logic

### Nguồn gốc: Recommended ANC contact schedule — đoạn 1394

WHO ANC recommendations (2016) (8)

### Nguồn gốc: ANC lab and tests schedule — đoạn 1395

Recommended schedule for conducting laboratory WHO ANC recommendations (2016): B.1.1, B.1.2, B.1.7, B.1.8 (8) tests and imaging if those tests are recommended WHO syphilis guideline for pregnant women (2017) (32) or required WHO syphilis PMTCT (IMPAC) (2007) (42)

### Nguồn gốc: Recommended schedule for counselling — đoạn 1396

recommended during pregnancy

### Nguồn gốc: Recommended schedule for counselling — đoạn 1397

WHO ANC recommendations (2016) (8) WHO syphilis guideline for pregnant women (2017) (32) WHO syphilis PMTCT (IMPAC) (2007) (42) Guidelines on hepatitis B and C testing (2017) (31) Medical eligibility criteria for contraceptive use (2015) (38) WHO pre-eclampsia and eclampsia recommendations (2011) (34)

### Nguồn gốc: Malaria prophylaxis — đoạn 1398

schedule

### Nguồn gốc: Recommended schedule for immunizations that — đoạn 1399

are recommended during pregnancy

### Nguồn gốc: Recommended schedule for immunizations that — đoạn 1400

Vaccines against influenza position paper (2012) (35)

### Nguồn gốc: Recommended schedule for malaria prophylaxis — đoạn 1401

recommended during pregnancy

### Nguồn gốc: Recommended schedule for malaria prophylaxis — đoạn 1402

WHO ANC recommendations (2016): C.6 (8)

### Nguồn gốc: Recommended schedule for malaria prophylaxis — đoạn 1403

WHO ANC recommendations (2016): C.5 (8)

### Nguồn gốc: Recommended schedule for malaria prophylaxis — đoạn 1404

requirements

### Nguồn gốc: Recommended schedule for malaria prophylaxis — đoạn 1405

indicators

### Nguồn gốc: Recommended schedule for malaria prophylaxis — đoạn 1406

decisions

### Nguồn gốc: Recommended schedule for malaria prophylaxis — đoạn 1407

data

### Nguồn gốc: Recommended schedule for malaria prophylaxis — đoạn 1408

WHO recommendations on health promotion interventions (2015) (39)

### Nguồn gốc: Component — đoạn 1409

personas

### Nguồn gốc: Indicators and — đoạn 1410

performance metrics

### Nguồn gốc: Percentage of — đoạn 1411

pregnant women with pregnant women who had their first ANC contact in first ANC contact the first trimester before 12 weeks (before 12 weeks of (facility level) gestation)

### Nguồn gốc: COUNT of — đoạn 1412

women whose gestational age ≤ 12 weeks at the time of first contact

### Nguồn gốc: COUNT of all women who — đoạn 1413

antenatal clients with had first contact within the last a first contact reporting period

### Nguồn gốc: Reference — đoạn 1414

Age (10–14, 15–19, 20+)

### Nguồn gốc: WHO ANC monitoring — đoạn 1415

framework (43)

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1416

guidance for RMNCAH don’t know, primary, programme managers secondary, higher) on the analysis and use of health-care facility data (18)

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1417

decisions

### Nguồn gốc: Indicator name — đoạn 1418

data

### Nguồn gốc: Indicator — đoạn 1419

code

### Nguồn gốc: Indicator — đoạn 1420

workflows

### Nguồn gốc: Indicator — đoạn 1421

Table 14. Indicators and performance metrics

### Nguồn gốc: Indicator — đoạn 1422

scenarios

### Nguồn gốc: Indicator — đoạn 1423

This section details indicators and performance metrics that would be aggregated from core data elements identified in Component 5. These indicators are based on the WHO ANC monitoring framework (43) and the WHO–UNICEF guidance for RMNCAH programme managers on the analysis and use of health-care facility data (18). These indicators may be aggregated automatically from the digital tracking tool to populate a digital HMIS, such as DHIS2. This table is also available in Web Annex C.

### Nguồn gốc: Computation — đoạn 1424

indicators

### Nguồn gốc: Computation — đoạn 1425

requirements

### Nguồn gốc: Percentage of — đoạn 1426

pregnant women who received iron and folic acid (IFA) supplements for 90+ days

### Nguồn gốc: WHO ANC monitoring — đoạn 1427

framework (43)

### Nguồn gốc: Number of — đoạn 1428

pregnant women who received the recommended number of IFA tablets during all previous contacts

### Nguồn gốc: COUNT of — đoạn 1429

antenatal clients with records were closed (ANC close number of form) in the last reporting period women who were a first contact due to any of the reasons below: prescribed IFA tablets at each » live birth

### Nguồn gốc: ANC contact they — đoạn 1430

» stillbirth have had » miscarriage

### Nguồn gốc: ANC contact they — đoạn 1431

Age (10–14, 15–19, 20+)

### Nguồn gốc: Percentage of — đoạn 1432

pregnant women screened for syphilis during ANC

### Nguồn gốc: COUNT of — đoạn 1433

antenatal clients with records were closed (ANC close number of a first contact form) in the last reporting period women who due to any of the reasons below: had at least one syphilis test result » live birth recorded during » stillbirth pregnancy » miscarriage

### Nguồn gốc: COUNT of — đoạn 1434

Age (10–14, 15–19, 20+)

### Nguồn gốc: Number of — đoạn 1435

pregnant women screened for syphilis

### Nguồn gốc: Number of — đoạn 1436

» abortion » woman died » lost to follow-up » moved away

### Nguồn gốc: Education level (none, — đoạn 1437

don’t know, primary, secondary, higher)

### Nguồn gốc: WHO ANC monitoring — đoạn 1438

framework (43)

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1439

guidance for RMNCAH don’t know, primary, programme managers secondary, higher) on the analysis and

### Nguồn gốc: Contact number — đoạn 1440

use of health-care categories (Contact facility data (18) number categories:* 1, 2–3, 4–8, 9+)

### Nguồn gốc: *If the woman had — đoạn 1441

multiple syphilis tests done during pregnancy, use the contact number of the first test result recorded

### Nguồn gốc: *If the woman had — đoạn 1442

data decisions

### Nguồn gốc: Reference — đoạn 1443

» abortion » woman died » lost to follow-up » moved away

### Nguồn gốc: ANC.IND.4 — đoạn 1444

indicators requirements

### Nguồn gốc: Definition — đoạn 1445

workflows

### Nguồn gốc: Definition — đoạn 1446

scenarios

### Nguồn gốc: Definition — đoạn 1447

personas

### Nguồn gốc: Indicator — đoạn 1448

code

### Nguồn gốc: Percentage of — đoạn 1449

pregnant women with at least four

### Nguồn gốc: Number of — đoạn 1450

pregnant women with four ANC contacts

### Nguồn gốc: COUNT of all — đoạn 1451

women who had antenatal clients with records were closed (ANC close form) in the last calendar month a total of at least a first contact due to any of the reasons below:

### Nguồn gốc: FOUR contacts — đoạn 1452

during pregnancy » live birth » stillbirth » miscarriage » abortion » woman died » lost to follow-up » moved away

### Nguồn gốc: FOUR contacts — đoạn 1453

Age (10–14, 15–19, 20+)

### Nguồn gốc: WHO ANC monitoring — đoạn 1454

framework (43)

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1455

guidance for RMNCAH don’t know, primary, programme managers secondary, higher) on the analysis and use of health-care facility data (18)

### Nguồn gốc: Percentage of — đoạn 1456

pregnant women with a minimum of eight antenatal care contacts

### Nguồn gốc: Number of — đoạn 1457

pregnant women with eight ANC contacts

### Nguồn gốc: COUNT of all — đoạn 1458

pregnant women women who with a first contact had a total of

### Nguồn gốc: EIGHT OR MORE — đoạn 1459

contacts during pregnancy

### Nguồn gốc: COUNT of all women whose — đoạn 1460

records were closed (ANC close form) in the last calendar month due to any of the reasons below:

### Nguồn gốc: Total number of — đoạn 1461

pregnant women with a first contact

### Nguồn gốc: COUNT of all women whose — đoạn 1462

records were closed (ANC close form) in the last calendar month due to any of the reasons below:

### Nguồn gốc: COUNT of all women whose — đoạn 1463

» live birth » stillbirth » miscarriage » abortion » woman died » lost to follow-up » moved away

### Nguồn gốc: COUNT of all women whose — đoạn 1464

Age (10–14, 15–19, 20+)

### Nguồn gốc: WHO ANC monitoring — đoạn 1465

framework (43)

### Nguồn gốc: COUNT of — đoạn 1466

antenatal clients with records were closed (ANC close all women a first contact form) in the last calendar month who had their due to any of the reasons below: blood pressure measured at » live birth least once during » stillbirth pregnancy » miscarriage

### Nguồn gốc: Education level (none, — đoạn 1467

don’t know, primary, secondary, higher)

### Nguồn gốc: Education level (none, — đoạn 1468

Age (10–14, 15–19, 20+)

### Nguồn gốc: WHO ANC monitoring — đoạn 1469

framework (43)

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1470

guidance for RMNCAH don’t know, primary, programme managers secondary, higher) on the analysis and Trimester categories: use of health facility » 1st (GA ≤ 12 weeks); data (18) » 2nd (GA > 12 weeks to ≤ 28 weeks); » 3rd (GA > 28 weeks)

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1471

requirements

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1472

indicators

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1473

» abortion » woman died » lost to follow-up » moved away

### Nguồn gốc: WHO ANC monitoring — đoạn 1474

framework (43)

### Nguồn gốc: WHO ANC monitoring — đoạn 1475

Age (10–14, 15–19, 20+)

### Nguồn gốc: WHO ANC monitoring — đoạn 1476

decisions

### Nguồn gốc: Percentage of — đoạn 1477

pregnant women with at least one blood pressure measure during ANC

### Nguồn gốc: Percentage of — đoạn 1478

» live birth » stillbirth » miscarriage » abortion » woman died » lost to follow-up » moved away

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1479

guidance for RMNCAH don’t know, primary, programme managers secondary, higher) on the analysis and use of health-care facility data (18)

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1480

data

### Nguồn gốc: COUNT of — đoạn 1481

all women who received counselling on danger signs

### Nguồn gốc: Pregnant women who Number of — đoạn 1482

received counselling pregnant women who received on danger signs (%) counselling on during at least one danger signs

### Nguồn gốc: Number of — đoạn 1483

antenatal clients with blood pressure measured

### Nguồn gốc: Disaggregation — đoạn 1484

workflows

### Nguồn gốc: Denominator — đoạn 1485

scenarios

### Nguồn gốc: Numerator — đoạn 1486

personas

### Nguồn gốc: Indicator — đoạn 1487

code

### Nguồn gốc: Percentage of — đoạn 1488

pregnant women with at least one blood pressure measure in the third trimester during ANC

### Nguồn gốc: WHO ANC monitoring — đoạn 1489

framework (43)

### Nguồn gốc: Number of — đoạn 1490

pregnant women with blood pressure measurement in third trimester

### Nguồn gốc: COUNT of — đoạn 1491

antenatal clients with records were closed (ANC close all women a first contact form) in the last calendar month who had their due to any of the reasons below: blood pressure measured at least » live birth once during their » stillbirth third trimester » miscarriage

### Nguồn gốc: COUNT of — đoạn 1492

Age (10–14, 15–19, 20+)

### Nguồn gốc: COUNT of all — đoạn 1493

antenatal clients with records were closed (ANC close women who a first contact form) in the last calendar month had their baby’s due to any of the reasons below: heartbeat listened to at least once » live birth during pregnancy » stillbirth

### Nguồn gốc: COUNT of all — đoạn 1494

Age (10–14, 15–19, 20+)

### Nguồn gốc: Education level (none, — đoạn 1495

don’t know, primary, secondary, higher)

### Nguồn gốc: Percentage of — đoạn 1496

pregnant women whose baby’s heartbeat was listened to at least once during ANC

### Nguồn gốc: Percentage of — đoạn 1497

pregnant women with an ultrasound scan before 24 weeks

### Nguồn gốc: Number of — đoạn 1498

pregnant women whose baby’s heartbeat was listened to

### Nguồn gốc: Number of — đoạn 1499

pregnant women who received ultrasound scan before 24 weeks

### Nguồn gốc: COUNT of all — đoạn 1500

women who had an ultrasound scan done before 24 weeks gestational age

### Nguồn gốc: Total number of — đoạn 1501

antenatal clients with gestational age equal to 24 weeks

### Nguồn gốc: Total number of — đoạn 1502

requirements

### Nguồn gốc: WHO ANC monitoring — đoạn 1503

framework (43)

### Nguồn gốc: Education level (none, — đoạn 1504

don’t know, primary, secondary, higher)

### Nguồn gốc: Education level (none, — đoạn 1505

» miscarriage » abortion » woman died » lost to follow-up » moved away

### Nguồn gốc: Education level (none, — đoạn 1506

Trimester categories:

### Nguồn gốc: COUNT of all pregnant — đoạn 1507

women who reached 24 weeks gestational age in the past calendar month

### Nguồn gốc: COUNT of all pregnant — đoạn 1508

Age (10–14, 15–19, 20+)

### Nguồn gốc: COUNT of all pregnant — đoạn 1509

» 1st (GA ≤ 12 weeks); » 2nd (GA > 12 weeks to ≤ 28 weeks); » 3rd (GA > 28 weeks)

### Nguồn gốc: Education level (none, — đoạn 1510

don’t know, primary, secondary, higher)

### Nguồn gốc: Education level (none, — đoạn 1511

indicators

### Nguồn gốc: Education level (none, — đoạn 1512

decisions

### Nguồn gốc: Education level (none, — đoạn 1513

data

### Nguồn gốc: Numerator — đoạn 1514

» abortion » woman died » lost to follow-up » moved away

### Nguồn gốc: Numerator — đoạn 1515

workflows

### Nguồn gốc: Numerator — đoạn 1516

scenarios

### Nguồn gốc: Numerator — đoạn 1517

personas

### Nguồn gốc: Indicator — đoạn 1518

code

### Nguồn gốc: WHO ANC monitoring — đoạn 1519

framework (43)

### Nguồn gốc: Percentage of — đoạn 1520

women who received three doses or more of intermittent preventive therapy for malaria (IPTp) during their last pregnancy

### Nguồn gốc: Number of — đoạn 1521

pregnant women given at least three doses of sulfadoxine– pyrimethamine for

### Nguồn gốc: COUNT of all — đoạn 1522

women who received a third dose of sulfadoxine– pyrimethamine for IPTp

### Nguồn gốc: Total number of — đoạn 1523

antenatal clients with records were closed (ANC close a first contact form) in the last calendar month due to any of the reasons below:

### Nguồn gốc: Percentage of — đoạn 1524

pregnant women counselled and tested for HIV

### Nguồn gốc: Number of — đoạn 1525

pregnant women attending ANC who received counselling and testing for HIV during pregnancy

### Nguồn gốc: COUNT of — đoạn 1526

all women who received provider-initiated counselling and testing (PICT)

### Nguồn gốc: COUNT of all women whose — đoạn 1527

antenatal clients with records were closed (ANC close a first contact form) in the last calendar month due to any of the reasons below:

### Nguồn gốc: COUNT of all women whose — đoạn 1528

Age (10–14, 15–19, 20+)

### Nguồn gốc: WHO ANC monitoring — đoạn 1529

framework (43)

### Nguồn gốc: COUNT of all women whose — đoạn 1530

women who antenatal clients with records were closed (ANC close received oral PrEP a first contact form) in the last calendar month due to any of the reasons below:

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1531

guidance for RMNCAH don’t know, primary, programme managers secondary, higher) on the analysis and use of health-care facility data (18)

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1532

Age (10–14, 15–19, 20+)

### Nguồn gốc: WHO ANC monitoring — đoạn 1533

framework (43)

### Nguồn gốc: Education level (none, — đoạn 1534

don’t know, primary, secondary, higher)

### Nguồn gốc: Education level (none, — đoạn 1535

indicators

### Nguồn gốc: Education level (none, — đoạn 1536

» live birth » stillbirth » miscarriage » abortion » woman died » lost to follow-up » moved away

### Nguồn gốc: WHO ANC monitoring — đoạn 1537

framework (43)

### Nguồn gốc: WHO ANC monitoring — đoạn 1538

decisions

### Nguồn gốc: Percentage of — đoạn 1539

pregnant women who pregnant women attending ANC received oral preexposure prophylaxis who received counselling and (PrEP) testing for HIV during pregnancy

### Nguồn gốc: Percentage of — đoạn 1540

» live birth » stillbirth » miscarriage » abortion » woman died » lost to follow-up » moved away

### Nguồn gốc: Percentage of — đoạn 1541

Age (10–14, 15–19, 20+)

### Nguồn gốc: Percentage of — đoạn 1542

data

### Nguồn gốc: ANC.IND.13 — đoạn 1543

» live birth » stillbirth » miscarriage » abortion » woman died » lost to follow-up » moved away

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1544

guidance for RMNCAH don’t know, primary, programme managers secondary, higher) on the analysis and use of health-care facility data (18)

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1545

ANC: antenatal care; GA: gestational age; RMNCAH: reproductive, maternal, newborn, child and adolescent health.

### Nguồn gốc: Education level (none, WHO–UNICEF — đoạn 1546

requirements

### Nguồn gốc: Indicators and performance metrics — đoạn 1547

workflows

### Nguồn gốc: Reference — đoạn 1548

scenarios

### Nguồn gốc: Disaggregation — đoạn 1549

personas

### Nguồn gốc: Indicator — đoạn 1550

code

### Nguồn gốc: recommendations — đoạn 1551

requirements

### Nguồn gốc: recommendations — đoạn 1552

indicators

### Nguồn gốc: recommendations — đoạn 1553

decisions

### Nguồn gốc: recommendations — đoạn 1554

data

### Nguồn gốc: recommendations — đoạn 1555

workflows

### Nguồn gốc: recommendations — đoạn 1556

scenarios

### Nguồn gốc: recommendations — đoạn 1557

personas

### Nguồn gốc: High-level functional — đoạn 1558

and non-functional requirements

### Nguồn gốc: High-level functional — đoạn 1559

This section provides an overview of illustrative functional and non-functional requirements that may be considered to kick-start the process of designing or adapting the digital tracking and decision-support system. Functional requirements describe the capabilities the system must have in order to meet the end-users’ needs and achieve tasks within the business process. Non-functional requirements provide the general attributes and features of the digital system to ensure usability and overcome technical and physical constraints. Examples of non-functional requirements include ability to work offline, multiple language settings and password protection.

### Nguồn gốc: High-level functional — đoạn 1560

Table 15 highlights some key functional requirements for executing the business processes listed in Component 4 of this document, and the complete set of functional requirements can be accessed in Web Annex D. Table 16 provides non-functional requirements as general characteristics of the overall system. Please note that these are not exhaustive lists and should be modified according to context and user persona needs.

### Nguồn gốc: 8.1 Functional requirements — đoạn 1561

Table 15. Functional requirementsa

### Nguồn gốc: Health worker or clerk — đoạn 1562

To bypass the standard flow at any point if an urgent case is identified

### Nguồn gốc: Health worker or clerk — đoạn 1563

To search to see whether client is already in the system (using at least 2 identifiers)

### Nguồn gốc: Health worker or clerk — đoạn 1564

The system to require me (a user) to search to see whether a client is already in the system prior to starting a new medical record entry

### Nguồn gốc: Health worker or clerk — đoạn 1565

To use client identification system (e.g. QR code, bar-code, fingerprint) and pull-up patient information

### Nguồn gốc: Health worker or clerk — đoạn 1566

To provide sufficient data to rule out the possibility that this client is already in the system

### Nguồn gốc: Health worker or clerk — đoạn 1567

The system to indicate mandatory fields that must be filled out for registration to be valid

### Nguồn gốc: Health worker or clerk — đoạn 1568

To enter a temporary identification in emergency situations when full identity unknown

### Nguồn gốc: Health worker or clerk — đoạn 1569

To display client information for validation (and be able to edit it)

### Nguồn gốc: Health worker or clerk — đoạn 1570

To be able to attach an identifier (e.g. QR code, bar-code, fingerprint, photo) to the client’s record based on national standards consent

### Nguồn gốc: Health worker or clerk — đoạn 1571

If this is a returning contact, to add the information to their previous contact

### Nguồn gốc: To record a time-and-date-stamped new contact (encounter) — đoạn 1572

workflows

### Nguồn gốc: I want… — đoạn 1573

scenarios

### Nguồn gốc: As a… — đoạn 1574

personas

### Nguồn gốc: Health worker or clerk — đoạn 1575

To bypass the standard flow at any point if an urgent case is identified; and to flag urgent cases to be seen promptly

### Nguồn gốc: Health worker or clerk — đoạn 1576

To indicate the woman has arrived for first contact, scheduled contact or a specific health concern (tasks will vary based on health worker roles)

### Nguồn gốc: Health worker — đoạn 1577

The system to provide a standardized form for the entry of ANC profiles

### Nguồn gốc: Health worker — đoạn 1578

To print a copy of the ANC information that replicates the paper antenatal register

### Nguồn gốc: Health worker — đoạn 1579

The system to provide a standardized form for the entry of clinical data

### Nguồn gốc: Health worker — đoạn 1580

The system to provide real-time range checks and integrity checks on data

### Nguồn gốc: To review past medical history previously entered — đoạn 1581

indicators

### Nguồn gốc: To review past medical history previously entered — đoạn 1582

requirements

### Nguồn gốc: High-level functional and non-functional requirements — đoạn 1583

decisions

### Nguồn gốc: Registration — đoạn 1584

data

### Nguồn gốc: recommendations — đoạn 1585

personas scenarios workflows data decisions indicators

### Nguồn gốc: Health worker — đoạn 1586

A list of tests that I (the health worker) can order

### Nguồn gốc: Health worker — đoạn 1587

To print a requisition for lab tests, which includes required information for performing the test

### Nguồn gốc: Health worker — đoạn 1588

To provide decision support as appropriate for users based on data entered

### Nguồn gốc: Health worker — đoạn 1589

The system to provide context-sensitive, real-time decision support in response to entry of clinical data (alerts, advice, resources)

### Nguồn gốc: Health worker — đoạn 1590

To provide decision support as appropriate for users based on data entered

### Nguồn gốc: Health worker — đoạn 1591

To provide context-specific information in response to the entry of clinical data

### Nguồn gốc: Health worker — đoạn 1592

To be able to input custom schedules to allow for contacts on specific days and times, account for holidays, etc.

### Nguồn gốc: Health worker — đoạn 1593

To display number of existing contacts per day (to allow for balancing)

### Nguồn gốc: Health worker — đoạn 1594

To indicate (based on protocol) the preferred days for follow-up contact

### Nguồn gốc: Health worker — đoạn 1595

To record identification and tracking information (e.g. mobile phone number) for patient in schedule

### Nguồn gốc: Self-care intervention in the home — đoạn 1596

or community

### Nguồn gốc: Woman — đoạn 1597

Access to accurate health information accessible and appropriate for lay persons (this may include both “pull” and “push” options)c

### Nguồn gốc: Self-care intervention in the home — đoạn 1598

or community

### Nguồn gốc: Woman — đoạn 1599

To receive information messages in a way that ensures my confidentiality

### Nguồn gốc: Health worker — đoạn 1600

A list of scheduled contacts to allow for defaulters to be traced

### Nguồn gốc: Health worker — đoạn 1601

The ability to “check in” a woman for a scheduled contact

### Nguồn gốc: To identify the date of the last attended contact — đoạn 1602

Business processes C and D are available in the full spreadsheet of “ANC functional and non-functional requirements”. The term “client” is used for the registration workflow as this is applicable to other health domains, and “women” is used for the ANC-specific business processes.

### Nguồn gốc: To identify the date of the last attended contact — đoạn 1603

c The forthcoming self-care interventions DAK will describe the requirements for client-facing digital tools that will access health information. a

### Nguồn gốc: To identify the date of the last attended contact — đoạn 1604

requirements

### Nguồn gốc: To identify the date of the last attended contact — đoạn 1605

b

### Nguồn gốc: 8.2 Non-functional requirements — đoạn 1606

Table 16. Non-functional requirements

### Nguồn gốc: Security – confidentiality — đoạn 1607

Provide a means to ensure confidentiality and privacy of personal health information

### Nguồn gốc: Security – authentication — đoạn 1608

Notify the user to change their password the first time they log in

### Nguồn gốc: Security – authentication — đoạn 1609

Lock a user out after a specified number of wrong password attempts

### Nguồn gốc: Security – authentication — đoạn 1610

Notify a user if their account is locked due to wrong password attempts

### Nguồn gốc: Security – audit trail and logs — đoạn 1611

Generate analysis of the usage of different system features and reports

### Nguồn gốc: Security – user management — đoạn 1612

Allow user with permission to create a new user and temporary password

### Nguồn gốc: Security – user management — đoạn 1613

Allow roles to be associated with specific geographical areas and/or health-care facilities

### Nguồn gốc: High-level functional and non-functional requirements — đoạn 1614

requirements

### Nguồn gốc: ANC.NFXNREQ.002 — đoạn 1615

indicators

### Nguồn gốc: Provide password-protected access for authorized users — đoạn 1616

decisions

### Nguồn gốc: Security – confidentiality — đoạn 1617

data

### Nguồn gốc: ANC.NFXNREQ.001 — đoạn 1618

workflows

### Nguồn gốc: Non-functional requirement — đoạn 1619

scenarios

### Nguồn gốc: Category — đoạn 1620

personas

### Nguồn gốc: recommendations — đoạn 1621

personas scenarios workflows data decisions indicators requirements

### Nguồn gốc: Security – user management — đoạn 1622

Allow each user to be assigned to one or more roles

### Nguồn gốc: Security – user management — đoạn 1623

Support definitions of unlimited roles and assigned levels of access, viewing, entry, editing and auditing

### Nguồn gốc: System requirements – general — đoạn 1624

Work in an environment that is subject to loss of connectivity

### Nguồn gốc: System requirements – general — đoạn 1625

Be designed to be flexible enough to accommodate necessary changes in the future

### Nguồn gốc: System requirements – general — đoạn 1626

Warn user if no valid backup for more than a predefined number of days

### Nguồn gốc: System requirements – general — đoạn 1627

Must have the ability to store images and other unstructured data

### Nguồn gốc: System requirements – scalability — đoạn 1628

Be able to accommodate at least [x number of] health-care facilities

### Nguồn gốc: System requirements – scalability — đoạn 1629

Be able to accommodate at least [x number of] concurrent users

### Nguồn gốc: System requirements – usability — đoạn 1630

Alert the user when navigating away from a form without saving

### Nguồn gốc: System requirements – usability — đoạn 1631

Support real-time data-entry validation and feedback to prevent data-entry errors from being recorded

### Nguồn gốc: System requirements – usability — đoạn 1632

Simplify data recording through predefined drop-down menu or searchable lists, radio buttons, check boxes

### Nguồn gốc: System requirements – usability — đoạn 1633

Use industry standard user interface practices and apply them consistently throughout the system

### Nguồn gốc: System requirements – usability — đoạn 1634

Easy to learn and intuitive to enable user to navigate between pages

### Nguồn gốc: System requirements – — đoạn 1635

configuration

### Nguồn gốc: System requirements – — đoạn 1636

configuration

### Nguồn gốc: System requirements – — đoạn 1637

Configure business rules in line with guidelines and standard operating procedures (SOPs)

### Nguồn gốc: System requirements – — đoạn 1638

configuration

### Nguồn gốc: System requirements – — đoạn 1639

configuration

### Nguồn gốc: System requirements – — đoạn 1640

interoperability

### Nguồn gốc: System requirements – — đoạn 1641

interoperability

### Nguồn gốc: System requirements – — đoạn 1642

interoperability

### Nguồn gốc: System requirements – — đoạn 1643

interoperability

### Nguồn gốc: System requirements – — đoạn 1644

interoperability

### Nguồn gốc: System requirements – — đoạn 1645

interoperability

### Nguồn gốc: System requirements – hardware — đoạn 1646

and connectivity

### Nguồn gốc: System requirements – hardware — đoạn 1647

Allow for data exchange and efficient synchronization across multiple facilities and points of service when Internet is available, even when it is intermittent and slow

### Nguồn gốc: System requirements – hardware — đoạn 1648

decisions

### Nguồn gốc: System requirements – hardware — đoạn 1649

Provide guidance to users to better support clinical guidelines and best clinical practices

### Nguồn gốc: System requirements – hardware — đoạn 1650

data

### Nguồn gốc: System requirements – usability — đoạn 1651

workflows

### Nguồn gốc: ANC.NFXNREQ.056 — đoạn 1652

scenarios

### Nguồn gốc: Non-functional requirement — đoạn 1653

personas

### Nguồn gốc: Requirement ID — đoạn 1654

indicators

### Nguồn gốc: Requirement ID — đoạn 1655

requirements

### Nguồn gốc: recommendations — đoạn 1656

personas scenarios

### Nguồn gốc: recommendations — đoạn 1657

Note: Terms in definitions also defined in this glossary shown in italics.

### Nguồn gốc: Business process — đoạn 1658

A set of related activities or tasks performed together to achieve the objectives of the health programme area, such as registration, counselling, referrals (1,15).

### Nguồn gốc: Data dictionary — đoạn 1659

A centralized repository of information about the data elements that contains their definition, relationships, origin, usage, and type of data. For this digital adaptation kit, the data dictionary is provided as a spreadsheet.

### Nguồn gốc: Data element — đoạn 1660

A unit of data that has specific and precise meaning.

### Nguồn gốc: Data element — đoạn 1661

Decision-support logic A set of decision rules for standard and exceptional cases that is separate from the business process. This would help reduce the complexity of the business process depiction without losing the detail necessary for coding the rules required for system functionality.

### Nguồn gốc: Decision support (for — đoạn 1662

health workers)

### Nguồn gốc: Decision support (for — đoạn 1663

decisions indicators requirements

### Nguồn gốc: Decision support (for — đoạn 1664

Digitized job aids that combine an individual’s health information with the health worker’s knowledge and clinical protocols to assist health workers in making diagnosis and treatment decisions (9).

### Nguồn gốc: Decision support (for — đoạn 1665

Decision-support table Decision-support tables are a semi-structured way to depict each discrete decision that will need to be embedded in the system. Depending on the complexity of the clinical guidelines, there will likely be multiple decision-support tables.

### Nguồn gốc: Digital health — đoạn 1666

The systematic application of information and communications technologies, computer science and data to support informed decision-making by individuals, the health-care workforce and health systems, to strengthen resilience to disease and improve health and wellness (44).

### Nguồn gốc: Digital tracking — đoạn 1667

The use of a digitized record to capture and store clients’ health information to enable follow-up of their health status and services received. This may include digital forms of paper-based registers and case management logs within specific target populations, as well as electronic medical records linked to uniquely identified individuals (9).

### Nguồn gốc: Functional — đoạn 1668

requirement

### Nguồn gốc: Functional — đoạn 1669

Capabilities the system must have in order to meet the end-users’ needs and achieve tasks within the business process.

### Nguồn gốc: Health information — đoạn 1670

system (HIS)

### Nguồn gốc: Health information — đoạn 1671

A system that integrates data collection, processing, reporting and use of the information necessary for improving health service effectiveness and efficiency through better management at all levels of health services (45).

### Nguồn gốc: Health management — đoạn 1672

information system

### Nguồn gốc: Health management — đoạn 1673

An information system specifically designed to assist in the management and planning of health programmes, as opposed to delivery of care (45).

### Nguồn gốc: Health management — đoạn 1674

data

### Nguồn gốc: Health management — đoạn 1675

workflows

### Nguồn gốc: Persona — đoạn 1676

A generic aggregate description of a person involved in or benefitting from a health programme.

### Nguồn gốc: Standard — đoạn 1677

In software, a standard is a specification used in digital application development that has been established, approved and published by an authoritative organization. These rules allow information to be shared and processed in a uniform, consistent manner independent of a particular application.

### Nguồn gốc: Task — đoạn 1678

A specific action in a business process.

### Nguồn gốc: Terminologies — đoạn 1679

For clinical care, terminologies are structured vocabularies covering health-related concepts – such as diseases, diagnoses, laboratory tests and treatments – to enable the storage, analysis and exchange of data in a consistent and standard way (46,47).

### Nguồn gốc: Workflow — đoạn 1680

A visual representation of the progression of activities (tasks, events, decision points) in a logical flow illustrating the interactions within the business process (16).

### Nguồn gốc: Workflow — đoạn 1681

workflows

### Nguồn gốc: Workflow — đoạn 1682

General attributes and features of the digital system to ensure usability and overcome technical and physical constraints. Examples of non-functional requirements include ability to work offline, multiple language settings, and password protection.

### Nguồn gốc: Workflow — đoạn 1683

scenarios

### Nguồn gốc: Non-functional — đoạn 1684

requirement

### Nguồn gốc: Non-functional — đoạn 1685

personas

### Nguồn gốc: Non-functional — đoạn 1686

The ability of different applications to access, exchange, integrate and use data in a coordinated manner through the use of shared application interfaces and standards, within and across organizational, regional and national boundaries, to provide timely and seamless portability of information and optimize health outcomes.

### Nguồn gốc: Interoperability — đoạn 1687

data decisions indicators requirements

### Nguồn gốc: Interoperability — đoạn 1688

requirements

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1689

indicators

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1690

decisions

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1691

data

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1692

workflows

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1693

scenarios

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1694

personas

### Nguồn gốc: recommendations — đoạn 1695

personas

### Nguồn gốc: recommendations — đoạn 1696

workflows

### Nguồn gốc: recommendations — đoạn 1697

data

### Nguồn gốc: recommendations — đoạn 1698

decisions

### Nguồn gốc: recommendations — đoạn 1699

indicators

### Nguồn gốc: recommendations — đoạn 1700

requirements

### Nguồn gốc: Health interventions and recommendations — đoạn 1701

scenarios

### Nguồn gốc: Annexes — đoạn 1702

annexes

### Nguồn gốc: ANNEX 1. Examples of detailed persona — đoạn 1703

Example 1: Dative, skilled midwife, 26 years old, speaks Kinyarwanda and English

### Nguồn gốc: Clinical tasks — đoạn 1704

• Receiving and orienting women in ANC

### Nguồn gốc: Clinical tasks — đoạn 1705

• ANC consultations

### Nguồn gốc: Clinical tasks — đoạn 1706

• Lab exams

### Nguồn gốc: Other tasks — đoạn 1707

• Filling registers (6 per each ANC day)

### Nguồn gốc: Other tasks — đoạn 1708

• Providing reports

### Nguồn gốc: My Typical Day — đoạn 1709

07h00: Receiving clients(Women and their partners), request them to fill their Insurance Papers 07h30: Counselling on 4 standards visits of ANC, Family Planning,

### Nguồn gốc: Nutrition,… — đoạn 1710

08h30: Taking Lab exams , VAT Vaccination, ANC Consultation by standard visit 13h00: Break 14h00: Giving results to clients 15h30: Closing, daily reporting, pointage

### Nguồn gốc: We fill too many — đoạn 1711

registers which is time consuming and creates a high turn around time

### Nguồn gốc: My workplace — đoạn 1712

• The health center is located in Muhima Sector and attached to

### Nguồn gốc: My workplace — đoạn 1713

Muhima hospital and serves 21588 people in its catchment area

### Nguồn gốc: My workplace — đoạn 1714

• At ANC visit day, we receive 15 to 20 couples

### Nguồn gốc: My workplace — đoạn 1715

• There is 1 nurse who works with me at each ANC day

### Nguồn gốc: My workplace — đoạn 1716

• There is a computer for our service, there is internet connection

### Nguồn gốc: My workplace — đoạn 1717

in our health facility.

### Nguồn gốc: PAPERWORK — đoạn 1718

•

### Nguồn gốc: Repetition of activities (registration same information in each — đoạn 1719

service like Names, DOB, Gender….) •

### Nguồn gốc: No medical history — đoạn 1720

•

### Nguồn gốc: Sometimes we are requested so — đoạn 1721

support other services, such as vaccinations

### Nguồn gốc: It is heavy because the number of — đoạn 1722

clients who come and service packages that I have to deliver to them is beyond my capacity.

### Nguồn gốc: Folic Acid(Fer) caused by the — đoạn 1723

stockout on district pharmacy level and we ask our clients to find them in private pharmacies.

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1724

annexes

### Nguồn gốc: Work — đoạn 1725

» Bulk of time spent running clinic (posyandu), attending births and writing reports » Proud of her position and impact » Relies on help from other cadres (kaders) to do things such as enter data, enter information, fetch clients and organize queues during clinic » Seems well organized » Takes own initiative to improve health in her community (she installed electricity at her own polindes [mobile clinic for maternal and child health and family planning] to have during deliveries) » Skill level among midwives (bidan) can vary, according to midwife coordinator; not all midwives are as good as Ahmla (similar situation in India)

### Nguồn gốc: Needs and goals — đoạn 1726

» Underwent formal education and internship

### Nguồn gốc: Needs and goals — đoạn 1727

» Needs fewer cases (all her time seems to be clinics and attending deliveries; she cannot possibly do this during “rush season”)

### Nguồn gốc: Needs and goals — đoạn 1728

» Organizes monthly clinic (posyandu) in each hamlet » Responsible for 24 hamlets » Leads ANC and health for children aged 0–1 with vaccination and nutrition nurses » Enters data into paper registers (individual, community) and completes monthly report forms that include indicator targets (from district health office) » Good technology skills – smartphone, some social media » Gets financial incentive for every delivery attended » Area has 200 pregnancies a year (Ahmla will attend up to 180); compounded by many happening at the same time of year

### Nguồn gốc: Annexes — đoạn 1729

» Sometimes role conflicts with dukun bayi (traditional unskilled birth attendant); noninvolvement of midwife (bidan) may be due to lack of desire, education or awareness [on the part of the pregnant woman] » Wants more leverage to convince families [of the value of having a trained and skilled midwife] » Wants dependable supplies » Finds tracking people and service difficult sometimes, especially if outside of the local health system

### Nguồn gốc: Annexes — đoạn 1730

annexes

### Nguồn gốc: Annexes — đoạn 1731

ANNEX 2. Guidance adding or amending data elements to the data dictionary When adapting the data dictionary, data elements may be needed to be modified or added due to existing paper registers or local reporting requirements. If starting from paper-based registers and forms, you can find additional guidance in the Handbook for digitizing primary health care (17) and below is an overview of the data mapping excerpt to provide a template for standardizing the data dictionary. When amending wording of data elements, it is important to ensure that the standard terminology codes still reflect the data element as originally intended.

### Nguồn gốc: Activity ID — đoạn 1732

The ID number of the task in the workflow in which the data element is collected. This will denote which point in time at which this data element is collected. This should align with the Activity ID that is provided in Form Mapping.

### Nguồn gốc: Data element code — đoạn 1733

Each data element should have an identification number or code that is unique across the entire project. Use existing serial or identification numbers when available. If no identifiers exist, fields should be enumerated in a logical format.

### Nguồn gốc: Form ID and form — đoạn 1734

data element label

### Nguồn gốc: Form ID and form — đoạn 1735

The Form ID should be from the ID listed. List the Form ID in which the data element appears. This is important for ensuring that the design of the digital system has taken into account all the required paper forms and data elements on those paper forms.

### Nguồn gốc: Form ID and form — đoạn 1736

Also list the label of the data element as written in the original form (or translated as closely as possible). This will be key in keeping track of which data elements from the original paper forms are duplicated. Note that duplicate data fields can be included purposely (patient identifiers, such as name, date of birth, village, ID number, etc. would be included in multiple data instruments as a means to identify an individual patient).

### Nguồn gốc: Data element label — đoạn 1737

The label of the data element written in a way in which the end-users can easily understand, e.g. “education level”, “weight”, “height”, “reasons for coming into facility”, “which medication(s) is your client taking?” The data element label in this column is what will be used in the digital form. The digital register should not simply replace the paper registers, but it should also streamline processes and link duplicated data elements; thus the data element label listed here should be what will be used in the digital system.

### Nguồn gốc: Data element name — đoạn 1738

The shorthand name for the data element (e.g. “educ_level” for “education level”; “weight_kg” for “weight”; “height_m” for “height”). This will be key when coding the system and determining calculations required. This data element name is what will reconcile any duplicate data elements in the digital system.

### Nguồn gốc: Description and — đoạn 1739

definition

### Nguồn gốc: Description and — đoạn 1740

The description of the data field, including any units that define the field (e.g. “weight in kilograms (kg)”). Provide a clear explanation of what this data point is requesting, assuming the reader has never seen the form. Be sure to use consistent and easy-to-understand terminology across all forms. This is particularly important if the data element name differs across forms but requires the same input.

### Nguồn gốc: Data type — đoạn 1741

The data type should be aligned with data types outlined by Fast Health Interoperability Resources (FHIR) standards. Some common data types are:

### Nguồn gốc: Data type — đoạn 1742

annexes

### Nguồn gốc: What to note — đoạn 1743

» Boolean (e.g. True/False, Yes/No) » String (a sequence of Unicode characters, e.g. name) » Date (e.g. date of birth) » Time (e.g. time of delivery) » ID (e.g. unique identifier assigned to the client) » Integer (a whole number, e.g. number of past pregnancies) » Decimal (rational numbers that have a decimal representation, e.g. exact duration of time, location coordinates, all percentages) » Observation (health status observations collected from the client, e.g. height, weight, eye colour, pulse, blood pressure, temperature, glucose level) » Codeable concept (a value that is usually supplied by providing a reference to one or more data points, e.g. body mass index, contraceptive prevalence rate) » Signature (an electronic representation of a signature that is either cryptographic or a graphical image that represents a signature or a signature process, e.g.

### Nguồn gốc: What to note — đoạn 1744

supervisor’s approval) » Attachment (additional data content defined in other formats, e.g. images) » Note that if there are multiple choice data fields, the “parent” data field should be labelled “Multiple choice – Select one” or “Multiple choice – Select all that apply”. Then each individual option should listed in the “Input” options column and be classified with one of the data types listed above.

### Nguồn gốc: What to note — đoạn 1745

Although the list above should be sufficient to relay this information to a health informaticist or technology vendor, there are many more data codes that can be applied to achieve a more precise classification. For other possible data types, please refer to the HL7 FHIR guide on data types (48).

### Nguồn gốc: Input options — đoạn 1746

For multiple choice fields only; otherwise leave this column blank. Write the list of responses from which the health worker may select. Each of these input options should be in a separate row and reference the parent Data ID in its own Data ID (e.g. [Parent data element ID].[Option number]). Each of these options should be labelled with a data type as indicated above.

### Nguồn gốc: Calculation — đoạn 1747

For “codeable concept” data type fields, write the formula that defines the field. Leave this column blank for all other data types. Use standard mathematical symbols and the data element label of the data element names included in the formula (e.g. for the body mass index (BMI) calculation, “weight_kg/ ([height_m]^2)”)

### Nguồn gốc: Validation required — đoạn 1748

“Yes” or “No” to indicate whether there needs to be some form of validation given the constraints provided by a range of acceptable responses.

### Nguồn gốc: Validation condition — đoạn 1749

The range of acceptable responses, if validation is required (e.g. for a phone number, only 10 digits allowed; for a birthday, only past dates allowed).

### Nguồn gốc: Editable — đoạn 1750

“Yes” or “No” to indicate whether the end-user, or health worker, would be able to edit the field after it has been input to the system.

### Nguồn gốc: Required — đoạn 1751

Note whether or not this field is: » Required – R

### Nguồn gốc: Required — đoạn 1752

» Required, but can be left empty – RE » Optional – O

### Nguồn gốc: Required — đoạn 1753

» Conditional on answers from other data fields – C » Conditional, but can be left empty – CE

### Nguồn gốc: Annexes — đoạn 1754

annexes

### Nguồn gốc: Reason for required — đoạn 1755

data

### Nguồn gốc: Reason for required — đoạn 1756

If this field is required (R), state the reason here – whether for: » Accountability for national-level reporting » Service delivery or clinical decision-making » Client identification The digital system should not simply replace your paper registers, but it should also streamline processes; thus, it is important to understand why a certain data field is actually required and seek opportunities to optimize data flows.

### Nguồn gốc: Reason for required — đoạn 1757

Given the high volume of data collection required of health service providers, it might be better to remove a data entry field if it serves no real purpose for the clinician, public health reporting, or any other identified purpose.

### Nguồn gốc: Skip logic — đoạn 1758

If this field is conditional on answers from other data fields (C) or conditional, but can be left empty (CE), denote what the skip logic is here. This is common for data elements that are a part of follow-up questions. If the input of one data element field has a value lower than a certain threshold, then some data inputs can be skipped. Those data elements will have skip logic that is defined by a pre-set threshold.

### Nguồn gốc: Linked to aggregate — đoạn 1759

indicator

### Nguồn gốc: Linked to aggregate — đoạn 1760

Aggregate indicators should be called out and specified. If this data element is linked to an aggregate indicator, then indicate “Yes” here. If not, indicate “No”.

### Nguồn gốc: Notes — đoạn 1761

If there is an issue or inconsistency in how a data field is defined, make a note of the issue here. Irregularities and inconsistencies will need to be resolved at a later stage through a process of team discussion and triangulation. This column should also be used for any other notes, annotations, or communication messages within the team.

### Nguồn gốc: Concept mappings — đoạn 1762

Depending on which systems you plan on interoperating with, other columns will most likely need to be added to map to the concepts used in the other system (e.g. ICD-11, SNOMED). One column should be used for each concept dictionary.

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1763

1.

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1764

2. 3. 4.

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1765

5. 6.

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1766

7.

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1767

8.

### Nguồn gốc: Digital Adaptation Kit for Antenatal Care — đoạn 1768

9.

### Nguồn gốc: Digital implementation investment guide (DIIG): integrating digital interventions into — đoạn 1769

health programmes. Geneva: World Health Organization; 2020 (https://www.who. int/publications/i/item/who-digital-implementation-investment-guide, accessed 18 September 2020).

### Nguồn gốc: 12. Boxwala AA, Rocha BH, Maviglia S, Kashyap V, Meltzer S, Kim J, et al. A multi-layered — đoạn 1770

framework for disseminating knowledge for computer-based decision support. J Am Med Inform Assoc. 2011;18:i132–i139. doi:10.1136/amiajnl-2011-000334.

### Nguồn gốc: 12. Boxwala AA, Rocha BH, Maviglia S, Kashyap V, Meltzer S, Kim J, et al. A multi-layered — đoạn 1771

Draft global strategy on digital health 2020–2025. Geneva: World Health

### Nguồn gốc: Organization; undated (https://www.who.int/docs/default-source/documents/ — đoạn 1772

gs4dhdaa2a9f352b0445bafbc79ca799dce4d.pdf, accessed 16 December 2020).

### Nguồn gốc: 14. UHC compendium: repository of interventions for universal health coverage. Geneva: — đoạn 1773

World Health Organization; 2020 (https://www.who.int/universal-health-coverage/ compendium/interventions-by-programme-area, accessed 17 December 2020).

### Nguồn gốc: 14. UHC compendium: repository of interventions for universal health coverage. Geneva: — đoạn 1774

Resolution WHA71.7: Digital health. In: Seventy-first World Health Assembly, 26 May

### Nguồn gốc: 14. UHC compendium: repository of interventions for universal health coverage. Geneva: — đoạn 1775

2018. (https://apps.who.int/gb/ebwha/pdf_files/WHA71/A71_R7-en.pdf, accessed

### Nguồn gốc: The roadmap for health measurement and accountability (MA4Health): a common — đoạn 1776

agenda for the post-2015 era. Geneva and Washington (DC): World Bank Group, United States Agency for International Development and World Health Organization; June 2015 (https://www.who.int/hrh/documents/roadmap4health_measurent_account/ en/, accessed 16 December 2020).

### Nguồn gốc: The roadmap for health measurement and accountability (MA4Health): a common — đoạn 1777

Digital strategy 2020–2024. [Washington (DC)]: United States Agency for International Development; undated (https://www.usaid.gov/sites/default/files/documents/15396/ USAID_Digital_Strategy.pdf, accessed 16 December 2020). The Global Fund strategic framework for data use for action and improvement at country level, 2017-2022. Geneva: The Global Fund to Fight AIDS, Tuberculosis and Malaria (https://www.theglobalfund.org/media/8362/me_ datauseforactionandimprovement_framework_en.pdf, accessed 16 December 2020).

### Nguồn gốc: WHO guideline: recommendations on digital interventions for health system — đoạn 1778

strengthening. Geneva: World Health Organization; 2019 (https://www.who.int/ reproductivehealth/publications/digital-interventions-health-system-strengthening/en/, accessed 8 October 2020). WHO recommendations on antenatal care for a positive pregnancy experience.

### Nguồn gốc: WHO guideline: recommendations on digital interventions for health system — đoạn 1779

Geneva: World Health Organization; 2016 (https://www.who.int/reproductivehealth/ publications/maternal_perinatal_health/anc-positive-pregnancy-experience/en/, accessed 8 October 2020).

### Nguồn gốc: WHO guideline: recommendations on digital interventions for health system — đoạn 1780

Classification of digital health interventions v1.0. (WHO/RHR/18.06). Geneva: World Health Organization; 2018 (https://www.who.int/reproductivehealth/publications/ mhealth/classification-digital-health-interventions/en/, accessed 8 October 2020).

### Nguồn gốc: 10. Barton, C, Kallem, C, Van Dyke, P, Mon, D, Richesson, R. Demonstrating “collect — đoạn 1781

once, use many” – assimilating public health secondary data use requirements into an existing Domain Analysis Model. AMIA Annu Symp Proc. AMIA Symposium, 2011, 98–107.

### Nguồn gốc: 13. PATH. Common requirements for maternal health information systems. [Seattle]: — đoạn 1782

[Program for Appropriate Technology in Health]; December 2012 (https://path. azureedge.net/media/documents/MCHN_mhis_crdm.pdf, accessed 16 November 2018).

### Nguồn gốc: 16. Collaborative Requirements Development Methodology (CRDM) [website]. Decatur — đoạn 1783

(GA): Public Health Informatics Institute, The Task Force for Global Health; 2016 (https://www.phii.org/crdm/, accessed 11 February 2021).

### Nguồn gốc: 17. Handbook for digitalizing primary health care: optimizing person-centred tracking and — đoạn 1784

decision-support systems across care pathways. Geneva: World Health Organization; in press (https://www.who.int/reproductivehealth/publications/handbook-digitalizingprimary-health-care/en/).

### Nguồn gốc: 18. WHO, UNICEF. Analysis and use of health facility data: guidance for RMNCAH — đoạn 1785

programme managers. (Working Document). Geneva: World Health Organization; October 2019 (https://www.who.int/healthinfo/tools_data_analysis_routine_facility/en/, accessed 18 September 2020).

### Nguồn gốc: WHO antenatal care recommendations adaptation toolkit: a standardised approach — đoạn 1786

for countries. Health Res Policy Sys. 2020;18:70. doi:10.1186/s12961-020-00554-4.

### Nguồn gốc: 20. National eHealth strategy toolkit. Geneva: World Health Organization and — đoạn 1787

International Telecommunication Union; 2012 (https://apps.who.int/iris/ handle/10665/75211, accessed 18 September 2020).

### Nguồn gốc: 21. Defining competent maternal and newborn health professionals. Background — đoạn 1788

document to the 2018 joint statement by WHO, UNFPA, UNICEF, ICM, ICN, FIGO and IPA: definition of skilled health personnel providing care during childbirth. Geneva: World Health Organization; 2018 (https://apps.who.int/iris/handle/10665/27281, accessed 14 June 2019).

### Nguồn gốc: 22. WHO recommendations: optimizing health worker roles to improve access to key — đoạn 1789

maternal and newborn health interventions through task shifting (OptimizeMNH). Geneva: World Health Organization; 2012 (https://www.optimizemnh.org, accessed 10 January 2019).

### Nguồn gốc: 23. Classifying health workers: mapping occupations to the international standard — đoạn 1790

classification. Geneva: World Health Organization; undated (https://www.who.int/hrh/ statistics/Health_workers_classification.pdf, accessed 18 September 2020).

### Nguồn gốc: 23. Classifying health workers: mapping occupations to the international standard — đoạn 1791

annexes
