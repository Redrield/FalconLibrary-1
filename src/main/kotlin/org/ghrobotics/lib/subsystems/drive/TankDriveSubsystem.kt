package org.ghrobotics.lib.subsystems.drive

/* ktlint-disable no-wildcard-imports */
import kotlinx.coroutines.runBlocking
import org.ghrobotics.lib.commands.*
import org.ghrobotics.lib.mathematics.twodim.control.TrajectoryFollower
import org.ghrobotics.lib.mathematics.twodim.geometry.Pose2dWithCurvature
import org.ghrobotics.lib.mathematics.twodim.geometry.Rectangle2d
import org.ghrobotics.lib.mathematics.twodim.trajectory.types.TimedTrajectory
import org.ghrobotics.lib.mathematics.twodim.trajectory.types.mirror
import org.ghrobotics.lib.mathematics.units.Length
import org.ghrobotics.lib.mathematics.units.Mass
import org.ghrobotics.lib.mathematics.units.second
import org.ghrobotics.lib.sensors.AHRSSensor
import org.ghrobotics.lib.utils.BooleanSource
import org.ghrobotics.lib.utils.Source
import org.ghrobotics.lib.utils.map
import org.ghrobotics.lib.wrappers.FalconSRX

abstract class TankDriveSubsystem : FalconSubsystem("Drive Subsystem") {
    abstract val leftMaster: FalconSRX<Length>
    abstract val rightMaster: FalconSRX<Length>

    abstract val ahrsSensor: AHRSSensor
    abstract val trajectoryFollower: TrajectoryFollower

    val localization = TankDriveLocalization()

    override fun lateInit() {
        runBlocking { localization.lateInit(this@TankDriveSubsystem) }
    }

    // Pre-generated Trajectory Methods

    fun followTrajectory(
        trajectory: TimedTrajectory<Pose2dWithCurvature>
    ) = FollowTrajectoryCommand(this, trajectory)

    fun followTrajectory(
        trajectory: TimedTrajectory<Pose2dWithCurvature>,
        pathMirrored: Boolean = false
    ) = followTrajectory(trajectory.let {
        if (pathMirrored) it.mirror() else it
    })

    fun followTrajectory(
        trajectory: Source<TimedTrajectory<Pose2dWithCurvature>>,
        pathMirrored: Boolean = false
    ) = FollowTrajectoryCommand(this, trajectory.map {
        if (pathMirrored) it.mirror() else it
    })

    fun followTrajectory(
        trajectory: TimedTrajectory<Pose2dWithCurvature>,
        pathMirrored: BooleanSource
    ) = followTrajectory(pathMirrored.map(trajectory.mirror(), trajectory))

    fun followTrajectory(
        trajectory: Source<TimedTrajectory<Pose2dWithCurvature>>,
        pathMirrored: BooleanSource
    ) = followTrajectory(pathMirrored.map(trajectory.map { it.mirror() }, trajectory))

    // Region conditional command methods

    fun withinRegion(region: Rectangle2d) =
        withinRegion(Source(region))

    fun withinRegion(region: Source<Rectangle2d>) =
        ConditionCommand { region().contains(localization.robotPosition.translation) }


    open fun characterizeDrive(wheelRadius: Length, trackWidthRadius: Length, robotMass: Mass): FalconCommandGroup =
        sequential {
            // ArrayLists to store raw data
            val linearVelocityData = ArrayList<CharacterizeVelocityCommand.Data>()
            val angularVelocityData = ArrayList<CharacterizeVelocityCommand.Data>()
            val linearAccelerationData = ArrayList<CharacterizeAccelerationCommand.Data>()
            val angularAccelerationData = ArrayList<CharacterizeAccelerationCommand.Data>()

            +CharacterizeVelocityCommand(this@TankDriveSubsystem, wheelRadius, false, linearVelocityData)
            +DelayCommand(2.second)
            +CharacterizeAccelerationCommand(this@TankDriveSubsystem, wheelRadius, false, linearAccelerationData)
            +DelayCommand(2.second)
            +CharacterizeVelocityCommand(this@TankDriveSubsystem, wheelRadius, true, angularVelocityData)
            +DelayCommand(2.second)
            +CharacterizeAccelerationCommand(this@TankDriveSubsystem, wheelRadius, true, angularAccelerationData)

            +InstantRunnableCommand {
                System.out.println(
                    CharacterizationCalculator.getDifferentialDriveConstants(
                        wheelRadius = wheelRadius,
                        effectiveWheelBaseRadius = trackWidthRadius,
                        robotMass = robotMass,
                        linearVelocityData = linearVelocityData,
                        angularVelocityData = angularVelocityData,
                        linearAccelerationData = linearAccelerationData,
                        angularAccelerationData = angularAccelerationData
                    )
                )
            }
        }
}