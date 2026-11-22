package ni.shikatu.pgupsd.domain

data class WebFaculty(
    val name: String,
    val path: String,
)

data class PlanLink(
    val year: Int,
    val form: String,
    val url: String,
)

data class WebProgram(
    val code: String,
    val direction: String,
    val level: String,
    val profile: String,
    val plans: List<PlanLink>,
)

data class PlanHours(
    val lecture: Int = 0,
    val lab: Int = 0,
    val practice: Int = 0,
    val srp: Int = 0,
    val selfStudy: Int = 0,
    val control: Int = 0,
) {
    val total: Int get() = lecture + lab + practice + srp + selfStudy + control
    val contact: Int get() = lecture + lab + practice + srp

    operator fun plus(other: PlanHours) = PlanHours(
        lecture = lecture + other.lecture,
        lab = lab + other.lab,
        practice = practice + other.practice,
        srp = srp + other.srp,
        selfStudy = selfStudy + other.selfStudy,
        control = control + other.control,
    )
}

data class PlanOffering(
    val semester: Int,
    val credits: Int,
    val hours: PlanHours,
)

enum class PlanBlock { CORE, VARIABLE, ELECTIVE, PRACTICE, GIA, EXTRA, OTHER }

data class PlanCourse(
    val id: String,
    val name: String,
    val block: PlanBlock,
    val inPlan: Boolean,
    val exams: List<Int>,
    val zachets: List<Int>,
    val courseProjects: List<Int>,
    val courseWorks: List<Int>,
    val offerings: List<PlanOffering>,
    val department: String,
) {
    val credits: Int get() = offerings.sumOf { it.credits }

    fun offering(semester: Int): PlanOffering? = offerings.find { it.semester == semester }
}

data class PlanMeta(
    val code: String,
    val direction: String,
    val profile: String,
    val faculty: String,
    val chair: String,
    val year: Int,
    val qualification: String,
    val form: String,
    val duration: String,
    val standard: String,
)

data class Curriculum(
    val meta: PlanMeta,
    val courses: List<PlanCourse>,
) {
    val semesters: List<Int> =
        courses.flatMap { it.offerings.map { offering -> offering.semester } }.distinct().sorted()
}
