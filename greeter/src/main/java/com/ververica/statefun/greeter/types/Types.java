package com.ververica.statefun.greeter.types;

import com.ververica.statefun.greeter.types.generated.UserProfile;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

public final class Types {

  private Types() {}

  private static final String TYPES_NAMESPACE = "greeter.types";

  public static final Type<UserProfile> USER_PROFILE_PROTOBUF_TYPE =
      SimpleType.simpleImmutableTypeFrom(
          TypeName.typeNameOf(TYPES_NAMESPACE, UserProfile.getDescriptor().getFullName()),
          UserProfile::toByteArray,
          UserProfile::parseFrom);
}
