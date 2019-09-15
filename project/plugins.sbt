addSbtPlugin("com.jsuereth"         % "sbt-pgp"               % "2.0.0")
addSbtPlugin("com.typesafe.sbt"     % "sbt-ghpages"           % "0.6.3")
addSbtPlugin("com.typesafe.sbt"     % "sbt-site"              % "1.3.3")
addSbtPlugin("org.scoverage"        % "sbt-scoverage"         % "1.6.0")
addSbtPlugin("com.typesafe.sbt"     % "sbt-git"               % "1.0.0")
addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"          % "2.6")
addSbtPlugin("ohnosequences"        % "sbt-github-release"    % "0.7.0")
addSbtPlugin("com.eed3si9n"         % "sbt-buildinfo"         % "0.9.0")

resolvers += "Era7 maven releases" at "http://releases.era7.com.s3.amazonaws.com"
